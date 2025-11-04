package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import lombok.Getter;

@Getter
public enum RegistrationEventType {
    MEMBER_REGISTERED("Registration"),
    CHILD_REGISTERED("Registration"),
    USER_INVITED("Registration");


    private final String aggregateType;

    RegistrationEventType(String aggregateType) {
        this.aggregateType = aggregateType;
    }
}
