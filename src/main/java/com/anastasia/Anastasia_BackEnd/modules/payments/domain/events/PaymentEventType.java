package com.anastasia.Anastasia_BackEnd.modules.payments.domain.events;

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

    public String getAggregateType() {
        return aggregateType;
    }
}

