package com.anastasia.Anastasia_BackEnd.core.notification.processors;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.InAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.SmsNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.WhatsAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationProcessor;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class GenericNotificationProcessor implements NotificationProcessor {

    private final EmailTemplateService emailTemplateService;
    private final InAppNotificationService inAppService;
    private final SmsNotificationService smsService;
    private final WhatsAppNotificationService whatsAppService;
    private final TenantRepository tenantRepository;
    private final ChurchRepository churchRepository;

    public GenericNotificationProcessor(EmailTemplateService emailTemplateService,
                                        InAppNotificationService inAppService,
                                        SmsNotificationService smsService,
                                        WhatsAppNotificationService whatsAppService,
                                        TenantRepository tenantRepository,
                                        ChurchRepository churchRepository) {
        this.emailTemplateService = emailTemplateService;
        this.inAppService = inAppService;
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
        this.tenantRepository = tenantRepository;
        this.churchRepository = churchRepository;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.NOTIFICATION;
    }

    @Override
    public void process(NotificationEvent event) {
        Map<String, Object> channelProps = new HashMap<>(event.getProperties());
        String title = readString(channelProps, "title", "New Notification");
        String subject = readString(channelProps, "subject", title);
        String message = readString(channelProps, "message_content", "You have a new notification from Anastasia.");
        channelProps.put("message_content", message);
        channelProps.putIfAbsent("username", event.getUser() != null ? event.getUser().getFullName() : "member");

        if (event.requiresChannel(com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType.EMAIL)
                && StringUtils.hasText(event.getTarget().email())) {
            Map<String, Object> emailModel = buildEmailModel(event, channelProps, title, subject, message);
            String html = emailTemplateService.renderHtml(EmailTemplate.ADMIN_MEMBER_NOTIFICATION.templateKey(), emailModel);
            String text = emailTemplateService.renderText(EmailTemplate.ADMIN_MEMBER_NOTIFICATION.templateKey(), emailModel);
            emailTemplateService.sendEmail(
                    event.getTarget().email(),
                    subject,
                    html,
                    text,
                    EmailSendMetadata.of(EmailCategory.ADMIN_ALERT, "admin-member-notification")
                            .withTenantId(resolveTenantId(event))
            );
        }

        smsService.send(event, SmsTemplateType.CUSTOM, channelProps);
        whatsAppService.send(event, null, channelProps);
        inAppService.send(event);
    }

    private String readString(Map<String, Object> properties, String key, String fallback) {
        Object value = properties.get(key);
        if (value == null) {
            return fallback;
        }
        String normalized = value.toString().trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private Map<String, Object> buildEmailModel(NotificationEvent event,
                                                Map<String, Object> channelProps,
                                                String title,
                                                String subject,
                                                String message) {
        Map<String, Object> model = new HashMap<>(channelProps);
        model.put("memberName", readString(channelProps, "username", "member"));
        model.put("messageTitle", title);
        model.put("messageContent", message);
        model.put("emailSubject", subject);

        resolveTenantBranding(event).ifPresent(branding -> {
            model.putIfAbsent("churchName", branding.churchName());
            model.putIfAbsent("churchLogoUrl", branding.churchLogoUrl());
            model.putIfAbsent("footerAddress", branding.footerAddress());
        });

        return model;
    }

    private Optional<TenantBranding> resolveTenantBranding(NotificationEvent event) {
        UUID tenantId = resolveTenantId(event);
        if (tenantId == null) {
            return Optional.empty();
        }

        Optional<TenantEntity> tenant = tenantRepository.findById(tenantId);
        Optional<ChurchEntity> church = churchRepository.findByTenantId(tenantId);

        String churchName = church.map(ChurchEntity::getChurchName)
                .filter(StringUtils::hasText)
                .orElseGet(() -> tenant.map(TenantEntity::getDisplayName).filter(StringUtils::hasText).orElse("Your Church"));
        String churchLogoUrl = church.map(ChurchEntity::getProfilePicture)
                .map(profile -> profile.getImageUrl())
                .filter(StringUtils::hasText)
                .orElse(null);
        String footerAddress = church.map(this::formatChurchAddress)
                .filter(StringUtils::hasText)
                .orElse(null);

        return Optional.of(new TenantBranding(churchName, churchLogoUrl, footerAddress));
    }

    private UUID resolveTenantId(NotificationEvent event) {
        UUID tenantId = event.getTarget().tenantId();
        if (tenantId == null && event.getUser() != null) {
            tenantId = event.getUser().getTenantId();
        }
        return tenantId;
    }

    private String formatChurchAddress(ChurchEntity church) {
        if (church.getAddress() == null) {
            return null;
        }

        ArrayList<String> parts = new ArrayList<>();
        addIfText(parts, church.getAddress().getAddressLine1());
        addIfText(parts, church.getAddress().getAddressLine2());
        addIfText(parts, church.getAddress().getCity());
        addIfText(parts, church.getAddress().getStateProvince());
        addIfText(parts, church.getAddress().getCountry());
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private void addIfText(ArrayList<String> parts, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(value.trim());
        }
    }

    private record TenantBranding(
            String churchName,
            String churchLogoUrl,
            String footerAddress
    ) {
    }
}
