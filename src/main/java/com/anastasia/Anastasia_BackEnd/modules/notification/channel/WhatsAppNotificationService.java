package com.anastasia.Anastasia_BackEnd.modules.notification.channel;

import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.modules.notification.repository.NotificationRepository;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class WhatsAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);

    private final NotificationRepository notificationRepository;
    private final boolean whatsappEnabled;
    private final String accountSid;
    private final String authToken;
    private final String whatsappFrom;

    public WhatsAppNotificationService(NotificationRepository notificationRepository,
                                       @Value("${notification.whatsapp.enabled:false}") boolean whatsappEnabled,
                                       @Value("${twilio.account_sid:}") String accountSid,
                                       @Value("${twilio.auth_token:}") String authToken,
                                       @Value("${twilio.whatsapp_number:}") String whatsappFrom) {
        this.notificationRepository = notificationRepository;
        this.whatsappEnabled = whatsappEnabled;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.whatsappFrom = whatsappFrom;
    }

    @PostConstruct
    void init() {
        if (!whatsappEnabled) {
            return;
        }

        if (!StringUtils.hasText(accountSid) || !StringUtils.hasText(authToken) || !StringUtils.hasText(whatsappFrom)) {
            throw new IllegalStateException("WhatsApp notifications enabled but Twilio credentials are missing");
        }

        Twilio.init(accountSid, authToken);
        log.info("Twilio WhatsApp channel enabled with sender {}", whatsappFrom);
    }

    @Async
    public void send(NotificationEvent event, String body, Map<String, Object> placeholders) {
        if (!event.requiresChannel(NotificationChannelType.WHATSAPP)) {
            return;
        }

        if (!whatsappEnabled) {
            log.debug("WhatsApp sending disabled, skipping event type={}", event.getType());
            return;
        }

        String destination = resolveDestination(event);
        if (!StringUtils.hasText(destination)) {
            log.debug("Skipping WhatsApp notification. No destination for user={}",
                    event.getUser() != null ? event.getUser().getEmail() : "anonymous");
            return;
        }

        String resolvedBody = body;
        if (!StringUtils.hasText(resolvedBody) && placeholders != null) {
            Object candidate = placeholders.get("message_content");
            resolvedBody = candidate != null ? candidate.toString() : "";
        }

        if (!StringUtils.hasText(resolvedBody)) {
            resolvedBody = "You have a new notification from Anastasia.";
        }

        try {
            Message response = Message.creator(
                    new PhoneNumber("whatsapp:" + destination),
                    new PhoneNumber("whatsapp:" + whatsappFrom),
                    resolvedBody
            ).create();

            NotificationEntity entity = new NotificationEntity();
            entity.setRecipientEmail(destination);
            entity.setTitle("WhatsApp Notification");
            entity.setMessage(resolvedBody);
            entity.setType(event.getType());
            entity.setChannel(NotificationChannelType.WHATSAPP);
            entity.setSent(true);
            entity.setSentAt(LocalDateTime.now());
            entity.setProviderMessageId(response.getSid());
            entity.setTenant(event.getUser() != null ? event.getUser().getTenant() : null);
            notificationRepository.save(entity);

            log.info("✅ WhatsApp message sent to {} (sid={})", destination, response.getSid());
        } catch (ApiException ex) {
            NotificationEntity entity = new NotificationEntity();
            entity.setRecipientEmail(destination);
            entity.setTitle("WhatsApp Notification");
            entity.setMessage(resolvedBody);
            entity.setType(event.getType());
            entity.setChannel(NotificationChannelType.WHATSAPP);
            entity.setSent(false);
            entity.setErrorMessage(ex.getMessage());
            entity.setTenant(event.getUser() != null ? event.getUser().getTenant() : null);
            notificationRepository.save(entity);

            log.error("Failed to send WhatsApp message to {}: {}", destination, ex.getMessage());
        }
    }

    private String resolveDestination(NotificationEvent event) {
        if (event.getTarget().whatsAppNumber() != null) {
            return event.getTarget().whatsAppNumber();
        }
        return event.getTarget().phoneNumber();
    }
}
