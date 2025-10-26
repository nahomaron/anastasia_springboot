package com.anastasia.Anastasia_BackEnd.modules.notification.processors;


import com.anastasia.Anastasia_BackEnd.modules.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.notification.channel.InAppNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.notification.channel.SmsNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.notification.channel.WhatsAppNotificationService;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.modules.notification.orchestrator.NotificationProcessor;
import com.anastasia.Anastasia_BackEnd.service.email.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.service.sms.SmsTemplateType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PasswordResetProcessor implements NotificationProcessor {

    private final EmailNotificationService emailService;
    private final InAppNotificationService inAppService;
    private final SmsNotificationService smsService;
    private final WhatsAppNotificationService whatsAppService;

    public PasswordResetProcessor(EmailNotificationService emailService,
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
        return NotificationType.PASSWORD_RESET;
    }

    @Override
    public void process(NotificationEvent event) {
        emailService.sendEmail(event, "Reset Your Password", EmailTemplateName.RESET_PASSWORD);

        Map<String, Object> channelProps = new HashMap<>(event.getProperties());
        channelProps.putIfAbsent("message_content", "Use the link to reset your Anastasia password.");

        smsService.send(event, SmsTemplateType.NOTIFICATION, channelProps);
        whatsAppService.send(event, null, channelProps);
        inAppService.send(event);
    }
}
