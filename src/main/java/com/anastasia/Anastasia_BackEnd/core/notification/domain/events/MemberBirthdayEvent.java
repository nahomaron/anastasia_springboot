package com.anastasia.Anastasia_BackEnd.core.notification.domain.events;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import lombok.Getter;

@Getter
public class MemberBirthdayEvent {
    private final Object source;
    private final Adult_MemberEntity member;

    public MemberBirthdayEvent(Object source, Adult_MemberEntity member) {
        this.source = source;
        this.member = member;
    }
}
