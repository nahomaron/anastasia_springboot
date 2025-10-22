package com.anastasia.Anastasia_BackEnd.notification.listener;

import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.notification.orchestrator.NotificationOrchestrator;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private final NotificationOrchestrator orchestrator;

    public NotificationListener(NotificationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        orchestrator.process(event);
    }
}

