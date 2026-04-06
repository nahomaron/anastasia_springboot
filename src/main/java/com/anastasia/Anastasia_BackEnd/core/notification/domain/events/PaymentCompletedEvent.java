package com.anastasia.Anastasia_BackEnd.core.notification.domain.events;

import lombok.Getter;

@Getter
public class PaymentCompletedEvent {
    private final Object source;
//    private final PaymentEntity payment;

    public PaymentCompletedEvent(Object source) {
        this.source = source;
    }
}

