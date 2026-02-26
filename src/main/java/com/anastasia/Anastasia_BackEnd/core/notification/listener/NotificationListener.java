package com.anastasia.Anastasia_BackEnd.core.notification.listener;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.orchestrator.NotificationOrchestrator;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationInboxService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Listens for NotificationEvent and delegates processing to NotificationOrchestrator.
 * notification bus entrypoint
 * listens for NotificationEvent and forwards it to the orchestrator.
 * This is the single place that consumes NotificationEvent and hands off to routing
 */
@Component
public class NotificationListener {

    private final NotificationOrchestrator orchestrator;
    private final NotificationInboxService notificationInboxService;

    public NotificationListener(NotificationOrchestrator orchestrator,
                                NotificationInboxService notificationInboxService) {
        this.orchestrator = orchestrator;
        this.notificationInboxService = notificationInboxService;
    }

    @EventListener
    @Async
    public void handleNotificationEvent(NotificationEvent event) {
        UUID userId = event.getUser() != null ? event.getUser().getUuid() : null;
        UUID tenantId = event.getUser() != null ? event.getUser().getTenantId() : null;

        Set<com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType> filteredChannels =
                notificationInboxService.filterChannels(tenantId, userId, event.getType(), event.getChannels());
        if (filteredChannels == null || filteredChannels.isEmpty()) {
            return;
        }

        NotificationEvent filteredEvent = new NotificationEvent(
                event.getSource(),
                event.getType(),
                event.getUser(),
                event.getProperties(),
                filteredChannels
        );
        orchestrator.process(filteredEvent);
    }
}
