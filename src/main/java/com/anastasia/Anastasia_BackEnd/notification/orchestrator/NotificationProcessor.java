package com.anastasia.Anastasia_BackEnd.notification.orchestrator;

import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.notification.domain.NotificationType;

public interface NotificationProcessor {
    NotificationType getType();
    void process(NotificationEvent event);
}
