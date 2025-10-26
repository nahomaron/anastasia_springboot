package com.anastasia.Anastasia_BackEnd.modules.notification.channel;

import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEntity;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.modules.notification.repository.NotificationRepository;
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

    public InAppNotificationService(SimpMessagingTemplate messagingTemplate,
                                    NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
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

        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientEmail(event.getUser().getEmail());
        entity.setTitle("New Notification");
        entity.setMessage(buildMessage(event));
        entity.setChannel(NotificationChannelType.IN_APP);
        entity.setType(event.getType());
        entity.setSent(true);
        entity.setSentAt(LocalDateTime.now());
        entity.setTenant(event.getUser().getTenant());
        notificationRepository.save(entity);

        // Send real-time via WebSocket topic
        messagingTemplate.convertAndSendToUser(
                event.getUser().getEmail(),
                "/queue/notifications",  // destination path
                entity
        );
    }

    private String buildMessage(NotificationEvent event) {
        return switch (event.getType()) {
            case ACCOUNT_ACTIVATION -> "Welcome " + event.getUser().getFullName() + "!";
            case PASSWORD_RESET -> "Your password reset request was processed.";
            case MEMBER_BIRTHDAY -> "Happy Birthday 🎉" + event.getUser().getFullName() + "!";
            default -> "You have a new notification.";
        };
    }
}
