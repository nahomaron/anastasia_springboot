package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

public enum OnboardingSessionStatus {
    DRAFT,
    CHECKOUT_CREATED,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    PROVISIONED,
    PROVISIONING_FAILED,
    EXPIRED,
    CANCELED,
    CHECKOUT_SKIPPED
}
