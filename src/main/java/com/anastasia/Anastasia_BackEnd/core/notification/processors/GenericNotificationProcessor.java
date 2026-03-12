package com.anastasia.Anastasia_BackEnd.core.notification.processors;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.InAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.SmsNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.WhatsAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationProcessor;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class GenericNotificationProcessor implements NotificationProcessor {

    private final EmailNotificationService emailService;
    private final InAppNotificationService inAppService;
    private final SmsNotificationService smsService;
    private final WhatsAppNotificationService whatsAppService;

    public GenericNotificationProcessor(EmailNotificationService emailService,
                                        InAppNotificationService inAppService,
                                        SmsNotificationService smsService,
                                        WhatsAppNotificationService whatsAppService) {
        this.emailService = emailService;
        this.inAppService = inAppService;
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
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
            emailService.sendEmail(
                    event.getTarget().email(),
                    subject,
                    buildHtml(title, message),
                    buildText(title, message),
                    EmailSendMetadata.of(EmailCategory.ADMIN_ALERT, "bulk-notification")
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

    private String buildHtml(String title, String message) {
        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeMessage = HtmlUtils.htmlEscape(message).replace("\n", "<br/>");
        return "<div><h2>" + safeTitle + "</h2><p>" + safeMessage + "</p></div>";
    }

    private String buildText(String title, String message) {
        return title + System.lineSeparator() + System.lineSeparator() + message;
    }
}
