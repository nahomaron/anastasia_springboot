package com.anastasia.Anastasia_BackEnd.core.notification.domain.events;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MemberBirthdayEvent extends ApplicationEvent {
    private final Adult_MemberEntity member;

    public MemberBirthdayEvent(Object source, Adult_MemberEntity member) {
        super(source);
        this.member = member;
    }

}

