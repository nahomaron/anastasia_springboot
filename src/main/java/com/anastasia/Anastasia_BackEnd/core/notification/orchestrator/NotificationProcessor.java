package com.anastasia.Anastasia_BackEnd.core.notification.orchestrator;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;

public interface NotificationProcessor {
    NotificationType getType();
    void process(NotificationEvent event);
}
