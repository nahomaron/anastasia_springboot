package com.anastasia.Anastasia_BackEnd.events;

import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UserRegisteredEvent extends ApplicationEvent {
    private final String userEmail;
    private final UserEntity user;

    public UserRegisteredEvent(Object source, UserEntity user) {
        super(source);
        this.userEmail = user.getEmail();
        this.user = user;
    }

}
