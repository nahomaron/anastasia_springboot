package com.anastasia.Anastasia_BackEnd.core.notification.domain.events;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.Getter;

@Getter
public class UserRegisteredEvent {
    private final Object source;
    private final String userEmail;
    private final UserEntity user;

    public UserRegisteredEvent(Object source, UserEntity user) {
        this.source = source;
        this.userEmail = user.getEmail();
        this.user = user;
    }
}
