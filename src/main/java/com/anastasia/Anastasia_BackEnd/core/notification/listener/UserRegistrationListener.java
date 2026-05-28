package com.anastasia.Anastasia_BackEnd.core.notification.listener;


import com.anastasia.Anastasia_BackEnd.common.config.PublicUrlUtils;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.events.UserRegisteredEvent;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserRegistrationListener {

    private final ApplicationEventPublisher publisher;
    @Value("${app.public.frontend-base-url:}")
    private String frontendBaseUrl;

    public UserRegistrationListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    // Listen for domain event from your auth service
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        UserEntity user = event.getUser();

        Map<String, Object> props = Map.of(
                "username", user.getFullName(),
                "confirmation_url", normalizeBaseUrl(frontendBaseUrl) + "/auth/activate"
        );

        publisher.publishEvent(
                new NotificationEvent(this, NotificationType.ACCOUNT_ACTIVATION, user, props)
        );
    }

    private String normalizeBaseUrl(String rawUrl) {
        return PublicUrlUtils.normalizeBaseUrl(rawUrl, "app.public.frontend-base-url");
    }
}
