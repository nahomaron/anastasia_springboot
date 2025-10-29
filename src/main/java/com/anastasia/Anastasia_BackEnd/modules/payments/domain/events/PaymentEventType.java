package com.anastasia.Anastasia_BackEnd.modules.payments.domain.events;

import lombok.Data;
import lombok.Getter;

/**
 * Enumeration of payment event types with their associated aggregate types.
 */
@Getter
public enum PaymentEventType {
    PAYMENT_INITIATED("Payment"),
    PAYMENT_AUTHORIZED("Payment"),
    PAYMENT_CAPTURED("Payment"),
    PAYMENT_FAILED("Payment"),

    SUBSCRIPTION_INITIATED("PaymentSubscription"),
    SUBSCRIPTION_ACTIVATED("PaymentSubscription"),
    SUBSCRIPTION_CANCELED("PaymentSubscription");

    private final String aggregateType;

    PaymentEventType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

}

