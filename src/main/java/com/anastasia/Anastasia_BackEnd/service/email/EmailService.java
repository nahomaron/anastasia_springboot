package com.anastasia.Anastasia_BackEnd.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Import Value annotation
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Autowired
    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(
            String to,
            String subject,
            EmailTemplateName emailTemplate,
            Map<String, Object> templateProperties // This is the key change: dynamic properties
    ) throws MessagingException {

        // Use the 'name' field from the EmailTemplateName enum, which directly corresponds
        // to your Thymeleaf template file names (e.g., "activate_account" for activate_account.html)
        String templateName = Optional.ofNullable(emailTemplate)
                .map(EmailTemplateName::getName) // Use the .getName() method from the enum
                .orElse("default_email_template"); // Provide a sensible default template name

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MULTIPART_MODE_MIXED,
                UTF_8.name()
        );

        // Create a Thymeleaf Context and set the dynamic properties
        Context context = new Context();
        if (templateProperties != null) {
            context.setVariables(templateProperties);
        }

        // Inject sender email from application properties
        //    @Value("${spring.mail.from}") // Use a default if property is not found
        String senderEmail = "info@anastasia.com";
        helper.setFrom(senderEmail); // Use the sender email injected from properties
        helper.setTo(to);
        helper.setSubject(subject);

        // Process the Thymeleaf template with the provided context
        String htmlContent = templateEngine.process(templateName, context);
        helper.setText(htmlContent, true); // 'true' indicates that the content is HTML

        mailSender.send(mimeMessage); // Send the email
    }
}
