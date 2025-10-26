package com.anastasia.Anastasia_BackEnd.modules.notification.orchestrator;

import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationType;

public interface NotificationProcessor {
    NotificationType getType();
    void process(NotificationEvent event);
}
