package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

public enum EntitlementActionType {
    PLAN_CHANGED,
    PLAN_OVERRIDE_GRANTED,
    PLAN_OVERRIDE_REVOKED,
    FEATURE_OVERRIDE_SET,
    FEATURE_OVERRIDE_REMOVED,
    PROMO_CREATED,
    PROMO_REDEEMED,
    PROMO_REVOKED
}
