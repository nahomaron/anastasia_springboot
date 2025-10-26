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
public class MemberBirthdayProcessor implements NotificationProcessor {

    private final EmailNotificationService emailService;
    private final InAppNotificationService inAppService;
    private final SmsNotificationService smsService;
    private final WhatsAppNotificationService whatsAppService;

    public MemberBirthdayProcessor(EmailNotificationService emailService,
                                   SmsNotificationService smsService,
                                   InAppNotificationService inAppService,
                                   WhatsAppNotificationService whatsAppService) {
        this.emailService = emailService;
        this.inAppService = inAppService;
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.MEMBER_BIRTHDAY;
    }

    @Override
    public void process(NotificationEvent event) {
        emailService.sendEmail(event, "🎉 Happy Birthday from Anastasia", EmailTemplateName.NOTIFICATION);

        Map<String, Object> channelProps = new HashMap<>(event.getProperties());
        String memberName = channelProps.getOrDefault("memberName",
                event.getUser() != null ? event.getUser().getFullName() : "there").toString();
        channelProps.putIfAbsent("message_content", "Happy Birthday " + memberName + " 🎉");

        smsService.send(event, SmsTemplateType.NOTIFICATION, channelProps);
        whatsAppService.send(event, null, channelProps);
        inAppService.send(event);
    }
}
