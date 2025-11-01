package com.anastasia.Anastasia_BackEnd.core.notification.listener;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationOrchestrator;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for NotificationEvent and delegates processing to NotificationOrchestrator.
 * notification bus entrypoint
 * listens for NotificationEvent and forwards it to the orchestrator.
 * This is the single place that consumes NotificationEvent and hands off to routing
 */
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

