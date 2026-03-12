package com.anastasia.Anastasia_BackEnd.core.notification.channel;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationDeliveryStatus;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationIdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class SmsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final ObjectProvider<SmsService> smsServiceProvider;
    private final NotificationRepository notificationRepository;
    private final NotificationIdempotencyService idempotencyService;
    private final boolean smsEnabled;

    public SmsNotificationService(ObjectProvider<SmsService> smsServiceProvider,
                                  NotificationRepository notificationRepository,
                                  NotificationIdempotencyService idempotencyService,
                                  @Value("${notification.sms.enabled:true}") boolean smsEnabled) {
        this.smsServiceProvider = smsServiceProvider;
        this.notificationRepository = notificationRepository;
        this.idempotencyService = idempotencyService;
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
        String idempotencyKey = idempotencyService.computeKey(event, NotificationChannelType.SMS, phoneNumber);
        if (idempotencyKey != null && notificationRepository.existsByIdempotencyKeyAndChannel(idempotencyKey, NotificationChannelType.SMS)) {
            log.debug("Skipping duplicate sms notification for key={}", idempotencyKey);
            return;
        }

        SmsService smsService = smsServiceProvider.getIfAvailable();
        if (smsService == null) {
            log.warn("No SmsService bean available. Skipping SMS delivery.");
            return;
        }

        CompletableFuture<Void> result = smsService.sendSms(phoneNumber, templateType, payload);
        result.whenComplete((ignored, throwable) -> {
            NotificationEntity entity = new NotificationEntity();
            Instant now = Instant.now();
            entity.setRecipientAddress(phoneNumber);
            entity.setTitle("SMS Notification");
            entity.setMessage(renderedBody);
            entity.setChannel(NotificationChannelType.SMS);
            entity.setType(event.getType());
            entity.setTenant(event.getUser() != null ? event.getUser().getTenant() : null);
            entity.setDeliveryStatus(throwable == null ? NotificationDeliveryStatus.SENT : NotificationDeliveryStatus.FAILED);
            entity.setProvider("SMS");
            entity.setProviderStatus(throwable == null ? "DELIVERED" : "FAILED");
            entity.setErrorMessage(throwable == null ? null : throwable.getMessage());
            entity.setErrorCode(throwable == null ? null : "SMS_DELIVERY_FAILED");
            entity.setRecipientUserId(event.getUser() != null ? event.getUser().getUuid() : null);
            entity.setIdempotencyKey(idempotencyKey);
            entity.setCorrelationId(idempotencyKey);
            entity.setRetryCount(throwable == null ? 0 : 1);
            entity.setLastAttemptAt(now);
            entity.setDeliveredAt(throwable == null ? now : null);
            entity.setFailedAt(throwable == null ? null : now);
            entity.setNextRetryAt(throwable == null ? null : now.plusSeconds(300));

            notificationRepository.save(entity);

            if (throwable == null) {
                log.info("✅ SMS dispatched to {} for type={}", phoneNumber, event.getType());
            } else {
                log.error("Failed to send SMS to {}: {}", phoneNumber, throwable.getMessage());
            }
        });
    }
}
