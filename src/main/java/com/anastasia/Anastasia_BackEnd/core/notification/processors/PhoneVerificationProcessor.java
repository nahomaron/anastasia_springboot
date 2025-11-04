package com.anastasia.Anastasia_BackEnd.core.notification.processors;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.SmsNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationProcessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PhoneVerificationProcessor implements NotificationProcessor {

    private final SmsNotificationService smsService;

    public PhoneVerificationProcessor(SmsNotificationService smsService) {
        this.smsService = smsService;
    }

    @Override
    public NotificationType getType() {
        return NotificationType.PHONE_VERIFICATION;
    }

    @Override
    public void process(NotificationEvent event) {
        Map<String, Object> props = new HashMap<>(event.getProperties());
        smsService.send(event, SmsTemplateType.OTP, props);
    }
}

