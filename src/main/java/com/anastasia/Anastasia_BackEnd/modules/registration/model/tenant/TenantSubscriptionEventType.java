package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

public enum TenantSubscriptionEventType {
    CREATED,
    PLAN_CHANGE_SCHEDULED,
    PLAN_CHANGED,
    STATUS_CHANGED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    CANCELED
}
