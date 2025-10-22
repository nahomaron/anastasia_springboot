package com.anastasia.Anastasia_BackEnd.notification.orchestrator;


import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationType;
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

        if (processor != null) {
            log.info("Processing notification type={} for user={}",
                    event.getType(), event.getUser().getEmail());
            processor.process(event);
        } else {
            log.warn("No processor found for event type={}, falling back to default handler",
                    event.getType());
            NotificationProcessor fallback = processorMap.get(NotificationType.NOTIFICATION);
            if (fallback != null) fallback.process(event);
        }
    }
}


