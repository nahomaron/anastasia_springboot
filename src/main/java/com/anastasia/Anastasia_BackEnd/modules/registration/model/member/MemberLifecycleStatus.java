package com.anastasia.Anastasia_BackEnd.modules.registration.model.member;

import java.util.Locale;

public enum MemberLifecycleStatus {
    PENDING,
    APPROVED,
    ACTIVE,
    NON_ACTIVE,
    DECEASED;

    public static MemberLifecycleStatus from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return MemberLifecycleStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
