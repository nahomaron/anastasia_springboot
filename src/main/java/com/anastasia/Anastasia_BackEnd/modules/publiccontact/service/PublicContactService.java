package com.anastasia.Anastasia_BackEnd.modules.publiccontact.service;

import com.anastasia.Anastasia_BackEnd.modules.publiccontact.dto.PublicContactRequest;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Service
public class PublicContactService {

    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_FILE_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "png", "jpg", "jpeg");

    private final JavaMailSender mailSender;
    private final TurnstileVerificationService turnstileVerificationService;
    private final String fromEmail;
    private final String recipientEmail;

    public PublicContactService(
            JavaMailSender mailSender,
            TurnstileVerificationService turnstileVerificationService,
            @Value("${spring.mail.from:noreply@anastasisapp.com}") String fromEmail,
            @Value("${app.public.contact.recipient-email:support@anastasia.app}") String recipientEmail
    ) {
        this.mailSender = mailSender;
        this.turnstileVerificationService = turnstileVerificationService;
        this.fromEmail = fromEmail;
        this.recipientEmail = recipientEmail == null ? "" : recipientEmail.trim();
    }

    public void submit(PublicContactRequest request, String remoteIp) {
        if (request.isTextPermission() && !StringUtils.hasText(request.getPhone())) {
            throw new IllegalArgumentException("Add a phone number if you want to receive text messages.");
        }
        if (!StringUtils.hasText(recipientEmail)) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Public contact intake is not configured on the server."
            );
        }

        turnstileVerificationService.verify(request.getTurnstileToken(), remoteIp);
        validateAttachment(request.getDocument());
        sendEmail(request);
    }

    private void sendEmail(PublicContactRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    request.getDocument() != null && !request.getDocument().isEmpty(),
                    UTF_8.name()
            );

            helper.setTo(recipientEmail);
            helper.setReplyTo(request.getEmail().trim());
            helper.setFrom(new InternetAddress(fromEmail.trim(), "Anastasis", UTF_8.name()));
            helper.setSubject(buildSubject(request));
            helper.setText(buildTextBody(request), buildHtmlBody(request));

            MultipartFile document = request.getDocument();
            if (document != null && !document.isEmpty()) {
                helper.addAttachment(safeFileName(document), document);
            }

            mailSender.send(mimeMessage);
            log.info("Public contact request submitted for church={} email={}",
                    request.getChurchName(), request.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send public contact request email", ex);
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Contact submission is temporarily unavailable."
            );
        }
    }

    private void validateAttachment(MultipartFile document) {
        if (document == null || document.isEmpty()) {
            return;
        }

        if (document.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment exceeds the 10 MB upload limit."
            );
        }

        String extension = extensionOf(document.getOriginalFilename());
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment type is not supported."
            );
        }
    }

    private String buildSubject(PublicContactRequest request) {
        return "[Public Contact] " + request.getTopic().trim() + " - " + request.getChurchName().trim();
    }

    private String buildTextBody(PublicContactRequest request) {
        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : "Not provided";
        String attachment = request.getDocument() != null && !request.getDocument().isEmpty()
                ? safeFileName(request.getDocument())
                : "None";

        return """
                New public contact request

                Topic: %s
                Full name: %s
                Email: %s
                Church name: %s
                Phone: %s
                Text permission: %s
                Attachment: %s

                Request description:
                %s
                """.formatted(
                request.getTopic().trim(),
                request.getFullName().trim(),
                request.getEmail().trim(),
                request.getChurchName().trim(),
                phone,
                request.isTextPermission() ? "Yes" : "No",
                attachment,
                request.getRequestDescription().trim()
        );
    }

    private String buildHtmlBody(PublicContactRequest request) {
        String attachment = request.getDocument() != null && !request.getDocument().isEmpty()
                ? safeFileName(request.getDocument())
                : "None";

        return """
                <html>
                  <body>
                    <h2>New public contact request</h2>
                    <p><strong>Topic:</strong> %s</p>
                    <p><strong>Full name:</strong> %s</p>
                    <p><strong>Email:</strong> %s</p>
                    <p><strong>Church name:</strong> %s</p>
                    <p><strong>Phone:</strong> %s</p>
                    <p><strong>Text permission:</strong> %s</p>
                    <p><strong>Attachment:</strong> %s</p>
                    <h3>Request description</h3>
                    <p>%s</p>
                  </body>
                </html>
                """.formatted(
                escape(request.getTopic()),
                escape(request.getFullName()),
                escape(request.getEmail()),
                escape(request.getChurchName()),
                escape(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : "Not provided"),
                request.isTextPermission() ? "Yes" : "No",
                escape(attachment),
                escape(request.getRequestDescription()).replace("\n", "<br/>")
        );
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value.trim());
    }

    private String safeFileName(MultipartFile document) {
        String original = document.getOriginalFilename();
        return StringUtils.hasText(original) ? original.trim() : "attachment";
    }

    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }
}
