package com.anastasia.Anastasia_BackEnd.core.notification.processors;


import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.InAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.SmsNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.WhatsAppNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationProcessor;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WelcomeProcessor implements NotificationProcessor {

    private final EmailNotificationService emailService;
    private final InAppNotificationService inAppService;
    private final SmsNotificationService smsService;
    private final WhatsAppNotificationService whatsAppService;

    public WelcomeProcessor(EmailNotificationService emailService,
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
        return NotificationType.WELCOME;
    }

    @Override
    public void process(NotificationEvent event) {
        emailService.sendEmail(event, "Welcome to Anastasia!", EmailTemplateName.WELCOME);

        Map<String, Object> channelProps = new HashMap<>(event.getProperties());
        channelProps.putIfAbsent("message_content", "Welcome to Anastasia! We're excited to have you.");

        smsService.send(event, SmsTemplateType.NOTIFICATION, channelProps);
        whatsAppService.send(event, null, channelProps);
        inAppService.send(event);
    }
}
