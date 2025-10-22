package com.anastasia.Anastasia_BackEnd.notification.channel;

import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.notification.template.TemplateService;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Enterprise-level email notification service built on top of Spring Mail + Thymeleaf.
 * Backward compatible with existing EmailService, but includes persistence metadata.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    private final NotificationRepository notificationRepository;
    private final boolean emailSendingEnabled;
    private final String defaultSenderEmail;

    public EmailNotificationService(JavaMailSender mailSender,
                                    TemplateService templateService,
                                    NotificationRepository notificationRepository,
                                    @Value("${notification.email.enabled:${email.sending.enabled:true}}") boolean emailSendingEnabled,
                                    @Value("${spring.mail.from:info@anastasia.com}") String defaultSenderEmail) {
        this.mailSender = mailSender;
        this.templateService = templateService;
        this.notificationRepository = notificationRepository;
        this.emailSendingEnabled = emailSendingEnabled;
        this.defaultSenderEmail = defaultSenderEmail;
    }

    @Async
    public void sendEmail(NotificationEvent event,
                          String subject,
                          EmailTemplateName template) {
        if (event != null && !event.requiresChannel(NotificationChannelType.EMAIL)) {
            return;
        }

        Map<String, Object> variables = new HashMap<>(event != null ? event.getProperties() : Map.of());
        if (event != null && event.getUser() != null) {
            variables.putIfAbsent("username", event.getUser().getFullName());
        }
        String to = event != null && event.getTarget().email() != null
                ? event.getTarget().email()
                : event != null && event.getUser() != null ? event.getUser().getEmail() : null;

        if (!StringUtils.hasText(to)) {
            log.warn("Unable to send email notification; no recipient for event type={}", event != null ? event.getType() : "N/A");
            return;
        }

        sendEmail(to, subject, template, variables, event);
    }

    @Async
    public void sendEmail(String to,
                          String subject,
                          EmailTemplateName template,
                          Map<String, Object> variables) {
        sendEmail(to, subject, template, variables, null);
    }

    private void sendEmail(String to,
                           String subject,
                           EmailTemplateName template,
                           Map<String, Object> variables,
                           NotificationEvent eventContext) {

        if (!emailSendingEnabled) {
            log.debug("Email sending disabled, skipping to={}", to);
            return;
        }

        Map<String, Object> payload = CollectionUtils.isEmpty(variables) ? new HashMap<>() : new HashMap<>(variables);

        try {
            String html = templateService.renderTemplate(template.getName(), payload);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, UTF_8.name());
            helper.setTo(to);
            helper.setFrom(defaultSenderEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("✅ Email sent to {} using template '{}'", to, template.getName());
            persistNotification(to, subject, html, template, eventContext, true, null);

        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage(), e);
            persistNotification(to, subject, null, template, eventContext, false, e.getMessage());
        }
    }

    private void persistNotification(String recipient,
                                     String subject,
                                     String body,
                                     EmailTemplateName template,
                                     NotificationEvent context,
                                     boolean success,
                                     String error) {
        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientEmail(recipient);
        entity.setTitle(subject);
        entity.setMessage(body);
        entity.setChannel(NotificationChannelType.EMAIL);
        entity.setType(context != null ? context.getType() : NotificationType.NOTIFICATION);
        entity.setSent(success);
        entity.setSentAt(success ? LocalDateTime.now() : null);
        entity.setErrorMessage(success ? null : error);
        if (context != null) {
            entity.setTenant(context.getUser() != null ? context.getUser().getTenant() : null);
        }
        notificationRepository.save(entity);
    }
}
