package com.anastasia.Anastasia_BackEnd.core.notification.orchestrator;


import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    private final Map<NotificationType, NotificationProcessor> processorMap = new EnumMap<>(NotificationType.class);

    public NotificationOrchestrator(List<NotificationProcessor> processors) {
        processors.forEach(p -> processorMap.put(p.getType(), p));
        log.info("✅ Registered {} notification processors", processors.size());
    }

    public void process(NotificationEvent event) {
        NotificationProcessor processor = processorMap.get(event.getType());

        String recipient = resolveRecipient(event);

        if (processor != null) {
            log.info("Processing notification type={} for recipient={}",
                    event.getType(), recipient);
            processor.process(event);
        } else {
            log.warn("No processor found for event type={}, falling back to default handler",
                    event.getType());
            NotificationProcessor fallback = processorMap.get(NotificationType.NOTIFICATION);
            if (fallback != null) {
                log.info("Delegating to fallback processor for recipient={}", recipient);
                fallback.process(event);
            }
        }
    }

    private String resolveRecipient(NotificationEvent event) {
        if (event.getUser() != null && event.getUser().getEmail() != null) {
            return event.getUser().getEmail();
        }
        if (event.getTarget() != null) {
            if (event.getTarget().email() != null) {
                return event.getTarget().email();
            }
            if (event.getTarget().phoneNumber() != null) {
                return event.getTarget().phoneNumber();
            }
        }
        return "anonymous";
    }
}

