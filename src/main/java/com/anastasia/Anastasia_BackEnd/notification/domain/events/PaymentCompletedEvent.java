package com.anastasia.Anastasia_BackEnd.notification.domain.events;

import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentCompletedEvent extends ApplicationEvent {
//    private final PaymentEntity payment;

    public PaymentCompletedEvent(Object source) {
        super(source);
    }
}


