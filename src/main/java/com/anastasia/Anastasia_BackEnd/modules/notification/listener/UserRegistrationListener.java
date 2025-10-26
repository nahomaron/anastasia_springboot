package com.anastasia.Anastasia_BackEnd.modules.notification.listener;


import com.anastasia.Anastasia_BackEnd.modules.notification.domain.events.UserRegisteredEvent;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.modules.notification.domain.NotificationType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserRegistrationListener {

    private final ApplicationEventPublisher publisher;

    public UserRegistrationListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    // Listen for domain event from your auth service
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        UserEntity user = event.getUser();

        Map<String, Object> props = Map.of(
                "username", user.getFullName(),
                "confirmation_url", "http://localhost:3000/activate-account"
        );

        publisher.publishEvent(
                new NotificationEvent(this, NotificationType.ACCOUNT_ACTIVATION, user, props)
        );
    }
}

