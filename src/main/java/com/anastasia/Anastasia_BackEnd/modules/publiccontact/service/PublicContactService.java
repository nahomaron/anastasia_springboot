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

import java.util.Map;
import java.util.Locale;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Service
public class PublicContactService {

    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;
    private static final int MAX_ATTACHMENT_FILENAME_LENGTH = 180;
    // This public upload path does not have malware scanning. Keep the accepted types narrow
    // until a scanning/quarantine pipeline exists.
    private static final Set<String> ALLOWED_FILE_EXTENSIONS =
            Set.of("pdf", "png", "jpg", "jpeg");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "pdf", Set.of("application/pdf"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg")
    );

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

        String fileName = normalizedOriginalFilename(document);
        if (!StringUtils.hasText(fileName)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment filename is required."
            );
        }

        if (fileName.length() > MAX_ATTACHMENT_FILENAME_LENGTH) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment filename is too long."
            );
        }

        if (containsControlCharacters(fileName)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment filename contains unsupported characters."
            );
        }

        if (document.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment exceeds the 10 MB upload limit."
            );
        }

        String extension = extensionOf(fileName);
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment type is not supported."
            );
        }

        String contentType = normalizedContentType(document);
        if (StringUtils.hasText(contentType) && !ALLOWED_CONTENT_TYPES.getOrDefault(extension, Set.of()).contains(contentType)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Attachment content type does not match the file extension."
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

        String lineSeparator = System.lineSeparator();
        return String.join(
                lineSeparator,
                "New public contact request",
                "",
                "Topic: " + request.getTopic().trim(),
                "Full name: " + request.getFullName().trim(),
                "Email: " + request.getEmail().trim(),
                "Church name: " + request.getChurchName().trim(),
                "Phone: " + phone,
                "Text permission: " + (request.isTextPermission() ? "Yes" : "No"),
                "Attachment: " + attachment,
                "",
                "Request description:",
                request.getRequestDescription().trim()
        );
    }

    private String buildHtmlBody(PublicContactRequest request) {
        String attachment = request.getDocument() != null && !request.getDocument().isEmpty()
                ? safeFileName(request.getDocument())
                : "None";

        String description = escape(request.getRequestDescription())
                .replace("\r\n", "\n")
                .replace("\n", "<br/>");

        return "<html>\n"
                + "  <body>\n"
                + "    <h2>New public contact request</h2>\n"
                + "    <p><strong>Topic:</strong> " + escape(request.getTopic()) + "</p>\n"
                + "    <p><strong>Full name:</strong> " + escape(request.getFullName()) + "</p>\n"
                + "    <p><strong>Email:</strong> " + escape(request.getEmail()) + "</p>\n"
                + "    <p><strong>Church name:</strong> " + escape(request.getChurchName()) + "</p>\n"
                + "    <p><strong>Phone:</strong> "
                + escape(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : "Not provided")
                + "</p>\n"
                + "    <p><strong>Text permission:</strong> " + (request.isTextPermission() ? "Yes" : "No") + "</p>\n"
                + "    <p><strong>Attachment:</strong> " + escape(attachment) + "</p>\n"
                + "    <h3>Request description</h3>\n"
                + "    <p>" + description + "</p>\n"
                + "  </body>\n"
                + "</html>\n";
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value.trim());
    }

    private String safeFileName(MultipartFile document) {
        String original = normalizedOriginalFilename(document);
        return StringUtils.hasText(original) ? original : "attachment";
    }

    private String extensionOf(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizedOriginalFilename(MultipartFile document) {
        String original = document.getOriginalFilename();
        if (!StringUtils.hasText(original)) {
            return "";
        }

        String normalized = original.replace('\\', '/').trim();
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1).trim() : normalized;
    }

    private String normalizedContentType(MultipartFile document) {
        return StringUtils.hasText(document.getContentType())
                ? document.getContentType().trim().toLowerCase(Locale.ROOT)
                : "";
    }

    private boolean containsControlCharacters(String value) {
        return value.chars().anyMatch(Character::isISOControl);
    }
}
