package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

public enum TenantSubscriptionUpgradeStatus {
    PENDING_CHECKOUT,
    CHECKOUT_COMPLETED,
    PAYMENT_CONFIRMED,
    CANCELED,
    EXPIRED,
    FAILED
}
