package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

public enum SubscriptionPlan {
    FREE,
    BASIC,
    ADVANCED,
    PREMIUM,
    ENTERPRISE,
    MONTHLY,
    ANNUAL;

    public int rank() {
        return switch (this) {
            case FREE -> 0;
            case BASIC -> 1;
            case ADVANCED -> 2;
            case PREMIUM, ENTERPRISE -> 3;
            case MONTHLY, ANNUAL -> 1;
        };
    }
}
