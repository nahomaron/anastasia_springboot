package com.anastasia.Anastasia_BackEnd.core.notification.listener;

import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

//    @EventListener
//    public void onPaymentReceived(PaymentCompletedEvent event) {
//        UserEntity payer = event.getPayer();
//
//        Map<String, Object> props = Map.of(
//                "amount", event.getAmount(),
//                "referenceId", event.getReferenceId(),
//                "receiptUrl", "http://localhost:3000/payments/" + event.getReferenceId()
//        );
//
//        publisher.publishEvent(
//                new NotificationEvent(this, NotificationType.PAYMENT_RECEIVED, payer, props)
//        );
//    }
}
