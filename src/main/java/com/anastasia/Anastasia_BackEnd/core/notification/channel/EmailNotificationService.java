package com.anastasia.Anastasia_BackEnd.core.notification.channel;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationDeliveryStatus;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationPreferenceEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationPreferenceRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.EmailSuppressionService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationIdempotencyService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.TenantEmailPolicyService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.core.notification.template.TemplateService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import jakarta.mail.internet.InternetAddress;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Enterprise-level email notification service built on top of Spring Mail + Thymeleaf.
 * Backward compatible with existing EmailService, but includes persistence metadata.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String DEFAULT_SENDER_NAME = "Anastasis";

    private final JavaMailSender mailSender;
    private final TemplateService templateService;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationIdempotencyService idempotencyService;
    private final EmailSuppressionService emailSuppressionService;
    private final TenantEmailPolicyService tenantEmailPolicyService;
    private final TenantRepository tenantRepository;
    private final boolean emailSendingEnabled;
    private final String defaultSenderEmail;

    public EmailNotificationService(JavaMailSender mailSender,
                                    TemplateService templateService,
                                    NotificationRepository notificationRepository,
                                    NotificationPreferenceRepository notificationPreferenceRepository,
                                    NotificationIdempotencyService idempotencyService,
                                    EmailSuppressionService emailSuppressionService,
                                    TenantEmailPolicyService tenantEmailPolicyService,
                                    TenantRepository tenantRepository,
                                    @Value("${notification.email.enabled:${email.sending.enabled:true}}") boolean emailSendingEnabled,
                                    @Value("${spring.mail.from:noreply@anastasisapp.com}") String defaultSenderEmail) {
        this.mailSender = mailSender;
        this.templateService = templateService;
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.idempotencyService = idempotencyService;
        this.emailSuppressionService = emailSuppressionService;
        this.tenantEmailPolicyService = tenantEmailPolicyService;
        this.tenantRepository = tenantRepository;
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

        sendEmail(to, subject, template, variables, event, null);
    }

    @Async
    public void sendEmail(String to,
                          String subject,
                          EmailTemplateName template,
                          Map<String, Object> variables,
                          UUID tenantId) {
        sendEmail(to, subject, template, variables, null, tenantId);
    }

    @Async
    public void sendEmail(String to,
                          String subject,
                          String html,
                          String text,
                          EmailSendMetadata metadata) {
        String idempotencyKey = metadata != null && StringUtils.hasText(metadata.idempotencyKey())
                ? metadata.idempotencyKey()
                : null;

        if (StringUtils.hasText(idempotencyKey)
                && notificationRepository.existsByIdempotencyKeyAndChannel(idempotencyKey, NotificationChannelType.EMAIL)) {
            log.debug("Skipping duplicate email notification for metadata idempotency key={}", idempotencyKey);
            return;
        }

        dispatchEmail(to, subject, html, text, metadata, null, idempotencyKey, null, null);
    }

    private void sendEmail(String to,
                           String subject,
                           EmailTemplateName template,
                           Map<String, Object> variables,
                           NotificationEvent eventContext,
                           UUID tenantIdOverride) {
        String idempotencyKey = idempotencyService.computeKey(eventContext, NotificationChannelType.EMAIL, to);
        if (idempotencyKey != null && notificationRepository.existsByIdempotencyKeyAndChannel(idempotencyKey, NotificationChannelType.EMAIL)) {
            log.debug("Skipping duplicate email notification for key={}", idempotencyKey);
            return;
        }

        Map<String, Object> payload = CollectionUtils.isEmpty(variables) ? new HashMap<>() : new HashMap<>(variables);
        String html = templateService.renderTemplate(template.getName(), payload);
        String text = renderTextFallback(payload, html);

        dispatchEmail(
                to,
                subject,
                html,
                text,
                null,
                eventContext,
                idempotencyKey,
                resolveEmailCategory(template),
                tenantIdOverride
        );
    }

    private void dispatchEmail(String to,
                               String subject,
                               String html,
                               String text,
                               EmailSendMetadata metadata,
                               NotificationEvent eventContext,
                               String idempotencyKey,
                               EmailCategory templateCategory,
                               UUID tenantIdOverride) {
        if (!emailSendingEnabled) {
            log.debug("Email sending disabled, skipping to={}", to);
            return;
        }

        if (!isEmailDeliveryAllowed(to, eventContext)) {
            return;
        }

        NotificationType notificationType = resolveNotificationType(eventContext, metadata);
        UUID tenantId = resolveTenantId(eventContext, metadata, tenantIdOverride);
        EmailCategory emailCategory = metadata != null ? metadata.category() : templateCategory;
        TenantEmailPolicyService.EmailPolicyDecision policyDecision =
                tenantEmailPolicyService.evaluate(tenantId, emailCategory, notificationType);
        if (!policyDecision.allowed()) {
            log.warn("Email blocked by tenant policy tenantId={} code={} to={}", tenantId, policyDecision.errorCode(), to);
            persistNotification(
                    to,
                    subject,
                    html,
                    eventContext,
                    metadata,
                    false,
                    policyDecision.errorMessage(),
                    policyDecision.errorCode(),
                    idempotencyKey,
                    tenantId,
                    notificationType
            );
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, UTF_8.name());
            helper.setTo(to);
            helper.setFrom(new InternetAddress(defaultSenderEmail.trim(), DEFAULT_SENDER_NAME, UTF_8.name()));
            helper.setSubject(subject);
            helper.setText(resolveTextBody(text, html), html);

            mailSender.send(mimeMessage);
            log.info("Email sent to {} template={} category={} correlationId={}",
                    to,
                    metadata != null ? metadata.templateKey() : "legacy",
                    metadata != null ? metadata.category() : null,
                    metadata != null ? metadata.correlationId() : null);

            persistNotification(to, subject, html, eventContext, metadata, true, null, null, idempotencyKey, tenantId, notificationType);

        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage(), e);
            persistNotification(
                    to,
                    subject,
                    html,
                    eventContext,
                    metadata,
                    false,
                    e.getMessage(),
                    "EMAIL_DELIVERY_FAILED",
                    idempotencyKey,
                    tenantId,
                    notificationType
            );
        }
    }

    private String resolveTextBody(String text, String html) {
        if (StringUtils.hasText(text)) {
            return text;
        }
        if (!StringUtils.hasText(html)) {
            return "";
        }
        return html
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<br\\s*/?>", "\\n")
                .replaceAll("(?is)</p>", "\\n\\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String renderTextFallback(Map<String, Object> variables, String html) {
        Object text = variables.get("plainText");
        if (text instanceof String plainText && StringUtils.hasText(plainText)) {
            return plainText;
        }
        return resolveTextBody(null, html);
    }

    private boolean isEmailDeliveryAllowed(String to, NotificationEvent eventContext) {
        if (!StringUtils.hasText(to)) {
            return false;
        }

        if (emailSuppressionService.isSuppressed(to)) {
            log.info("Skipping email delivery because recipient is suppressed");
            return false;
        }

        if (eventContext == null || eventContext.getUser() == null) {
            return true;
        }

        UUID userId = eventContext.getUser().getUuid();
        if (userId == null) {
            return true;
        }

        Optional<NotificationPreferenceEntity> preference = findPreference(eventContext.getUser().getTenantId(), userId);
        if (preference.isEmpty()) {
            return true;
        }

        NotificationPreferenceEntity pref = preference.get();
        if (!pref.isEmailEnabled()) {
            log.info("Skipping email delivery because user email notifications are disabled");
            return false;
        }

        return pref.getMutedTypes() == null
                || eventContext.getType() == null
                || !pref.getMutedTypes().contains(eventContext.getType());
    }

    private Optional<NotificationPreferenceEntity> findPreference(UUID tenantId, UUID userId) {
        if (tenantId == null) {
            return notificationPreferenceRepository.findByTenantIdIsNullAndUserId(userId);
        }
        return notificationPreferenceRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    private void persistNotification(String recipient,
                                     String subject,
                                     String body,
                                     NotificationEvent context,
                                     EmailSendMetadata metadata,
                                     boolean success,
                                     String error,
                                     String errorCode,
                                     String idempotencyKey,
                                     UUID tenantId,
                                     NotificationType notificationType) {
        NotificationEntity entity = new NotificationEntity();
        Instant now = Instant.now();
        entity.setRecipientAddress(recipient);
        entity.setTitle(subject);
        entity.setMessage(body);
        entity.setChannel(NotificationChannelType.EMAIL);
        entity.setType(notificationType != null ? notificationType : resolveNotificationType(context, metadata));
        entity.setDeliveryStatus(success ? NotificationDeliveryStatus.SENT : NotificationDeliveryStatus.FAILED);
        entity.setProvider("AWS_SES");
        entity.setProviderStatus(success ? "DELIVERED" : "FAILED");
        entity.setErrorMessage(success ? null : error);
        entity.setErrorCode(success ? null : errorCode);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setCorrelationId(metadata != null && StringUtils.hasText(metadata.correlationId())
                ? metadata.correlationId()
                : idempotencyKey);
        entity.setProviderMessageId(metadata != null && StringUtils.hasText(metadata.correlationId())
                ? metadata.correlationId()
                : null);
        entity.setRetryCount(success ? 0 : 1);
        entity.setLastAttemptAt(now);
        entity.setDeliveredAt(success ? now : null);
        entity.setFailedAt(success ? null : now);
        entity.setNextRetryAt(success ? null : now.plusSeconds(300));
        if (context != null) {
            entity.setTenant(context.getUser() != null ? context.getUser().getTenant() : null);
            entity.setRecipientUserId(context.getUser() != null ? context.getUser().getUuid() : null);
        } else if (tenantId != null) {
            entity.setTenant(tenantRepository.findById(tenantId).orElse(null));
        }
        notificationRepository.save(entity);
    }

    private UUID resolveTenantId(NotificationEvent eventContext,
                                 EmailSendMetadata metadata,
                                 UUID tenantIdOverride) {
        if (tenantIdOverride != null) {
            return tenantIdOverride;
        }
        if (metadata != null && metadata.tenantId() != null) {
            return metadata.tenantId();
        }
        if (eventContext != null && eventContext.getUser() != null) {
            return eventContext.getUser().getTenantId();
        }
        return null;
    }

    private EmailCategory resolveEmailCategory(EmailTemplateName template) {
        if (template == null) {
            return null;
        }
        return switch (template) {
            case ACTIVATE_ACCOUNT -> EmailCategory.AUTH;
            case RESET_PASSWORD -> EmailCategory.SECURITY;
            case PAYMENT_RECEIPT, SUBSCRIPTION_ACTIVATED, SUBSCRIPTION_CANCELED -> EmailCategory.BILLING;
            default -> null;
        };
    }

    private NotificationType resolveNotificationType(NotificationEvent context, EmailSendMetadata metadata) {
        if (context != null && context.getType() != null) {
            return context.getType();
        }
        if (metadata == null || metadata.category() == null) {
            return NotificationType.NOTIFICATION;
        }

        return switch (metadata.category()) {
            case AUTH -> NotificationType.ACCOUNT_ACTIVATION;
            case SECURITY -> NotificationType.PASSWORD_RESET;
            case MEMBER -> NotificationType.MEMBER_REGISTRATION_SUBMITTED;
            case EVENT -> NotificationType.EVENT_REMINDER;
            case APPOINTMENT, FORM, TENANT, ADMIN_ALERT, BILLING, SYSTEM, COMPLIANCE -> NotificationType.NOTIFICATION;
        };
    }
}
