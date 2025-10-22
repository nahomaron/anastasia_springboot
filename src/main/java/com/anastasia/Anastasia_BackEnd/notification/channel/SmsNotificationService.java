package com.anastasia.Anastasia_BackEnd.notification.channel;

import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.service.sms.SmsService;
import com.anastasia.Anastasia_BackEnd.service.sms.SmsTemplateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final ObjectProvider<SmsService> smsServiceProvider;
    private final NotificationRepository notificationRepository;
    private final boolean smsEnabled;

    public SmsNotificationService(ObjectProvider<SmsService> smsServiceProvider,
                                  NotificationRepository notificationRepository,
                                  @Value("${notification.sms.enabled:true}") boolean smsEnabled) {
        this.smsServiceProvider = smsServiceProvider;
        this.notificationRepository = notificationRepository;
        this.smsEnabled = smsEnabled;
    }

    @Async
    public void send(NotificationEvent event,
                     SmsTemplateType templateType,
                     Map<String, Object> overrides) {

        if (!event.requiresChannel(NotificationChannelType.SMS)) {
            return;
        }

        if (!smsEnabled) {
            log.debug("SMS sending disabled. Skipping type={} user={}", event.getType(),
                    event.getUser() != null ? event.getUser().getEmail() : "anonymous");
            return;
        }

        String phoneNumber = event.getTarget().phoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            log.debug("Skipping SMS notification. No phone number for user={}",
                    event.getUser() != null ? event.getUser().getEmail() : "anonymous");
            return;
        }

        Map<String, Object> payload = new HashMap<>(event.getProperties());
        if (!CollectionUtils.isEmpty(overrides)) {
            payload.putAll(overrides);
        }
        if (event.getUser() != null) {
            payload.putIfAbsent("username", event.getUser().getFullName());
        }

        String renderedBody = templateType == SmsTemplateType.CUSTOM
                ? payload.getOrDefault("message_content", "").toString()
                : templateType.format(payload);

        SmsService smsService = smsServiceProvider.getIfAvailable();
        if (smsService == null) {
            log.warn("No SmsService bean available. Skipping SMS delivery.");
            return;
        }

        CompletableFuture<Void> result = smsService.sendSms(phoneNumber, templateType, payload);
        result.whenComplete((ignored, throwable) -> {
            NotificationEntity entity = new NotificationEntity();
            entity.setRecipientEmail(phoneNumber);
            entity.setTitle("SMS Notification");
            entity.setMessage(renderedBody);
            entity.setChannel(NotificationChannelType.SMS);
            entity.setType(event.getType());
            entity.setTenant(event.getUser() != null ? event.getUser().getTenant() : null);
            entity.setSent(throwable == null);
            entity.setSentAt(throwable == null ? LocalDateTime.now() : null);
            entity.setErrorMessage(throwable == null ? null : throwable.getMessage());

            notificationRepository.save(entity);

            if (throwable == null) {
                log.info("✅ SMS dispatched to {} for type={}", phoneNumber, event.getType());
            } else {
                log.error("Failed to send SMS to {}: {}", phoneNumber, throwable.getMessage());
            }
        });
    }
}
