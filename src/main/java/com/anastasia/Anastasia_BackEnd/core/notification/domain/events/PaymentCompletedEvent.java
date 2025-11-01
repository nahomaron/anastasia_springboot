package com.anastasia.Anastasia_BackEnd.core.notification.domain.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {
//    private final PaymentEntity payment;

    public PaymentCompletedEvent(Object source) {
        super(source);
    }
}


