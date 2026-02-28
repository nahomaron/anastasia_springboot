package com.anastasia.Anastasia_BackEnd.core.notification.channel;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationDeliveryStatus;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationRealtimeMessage;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.NotificationRepository;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationIdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationService.class);

    private final SimpMessagingTemplate messagingTemplate; // Spring WebSocket abstraction
    private final NotificationRepository notificationRepository;
    private final NotificationIdempotencyService idempotencyService;

    public InAppNotificationService(SimpMessagingTemplate messagingTemplate,
                                    NotificationRepository notificationRepository,
                                    NotificationIdempotencyService idempotencyService) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.idempotencyService = idempotencyService;
    }

    @Async
    public void send(NotificationEvent event) {
        if (!event.requiresChannel(NotificationChannelType.IN_APP)) {
            return;
        }

        if (event.getUser() == null) {
            log.warn("Skipping in-app notification because user context is missing for type={}", event.getType());
            return;
        }

        String recipient = event.getUser().getEmail();
        String idempotencyKey = idempotencyService.computeKey(event, NotificationChannelType.IN_APP, recipient);
        if (idempotencyKey != null && notificationRepository.existsByIdempotencyKeyAndChannel(idempotencyKey, NotificationChannelType.IN_APP)) {
            log.debug("Skipping duplicate in-app notification for key={}", idempotencyKey);
            return;
        }

        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientEmail(recipient);
        entity.setTitle("New Notification");
        entity.setMessage(buildMessage(event));
        entity.setChannel(NotificationChannelType.IN_APP);
        entity.setType(event.getType());
        entity.setSent(true);
        entity.setSentAt(LocalDateTime.now());
        entity.setDeliveryStatus(NotificationDeliveryStatus.SENT);
        entity.setRecipientUserId(event.getUser().getUuid());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setRetryCount(0);
        entity.setTenant(event.getUser().getTenant());
        NotificationEntity saved = notificationRepository.save(entity);
        NotificationRealtimeMessage payload = new NotificationRealtimeMessage(
                saved.getId(),
                saved.getTitle(),
                saved.getMessage(),
                saved.getType(),
                saved.getCreatedAt(),
                false
        );

        // Send real-time via WebSocket topic
        messagingTemplate.convertAndSendToUser(
                recipient,
                "/queue/notifications",  // destination path
                payload
        );
    }

    private String buildMessage(NotificationEvent event) {
        return switch (event.getType()) {
            case ACCOUNT_ACTIVATION -> "Welcome " + event.getUser().getFullName() + "!";
            case PASSWORD_RESET -> "Your password reset request was processed.";
            case MEMBER_BIRTHDAY -> "Happy Birthday 🎉" + event.getUser().getFullName() + "!";
            case MEMBER_REGISTRATION_SUBMITTED ->
                    "New member registration submitted: " + event.getProperties().getOrDefault("memberName", "Unknown member");
            case CHILD_REGISTRATION_SUBMITTED ->
                    "New child registration submitted: " + event.getProperties().getOrDefault("childName", "Unknown child");
            default -> "You have a new notification.";
        };
    }
}
