package com.anastasia.Anastasia_BackEnd.core.notification.domain.events;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MemberBirthdayEvent extends ApplicationEvent {
    private final MemberEntity member;

    public MemberBirthdayEvent(Object source, MemberEntity member) {
        super(source);
        this.member = member;
    }

}

