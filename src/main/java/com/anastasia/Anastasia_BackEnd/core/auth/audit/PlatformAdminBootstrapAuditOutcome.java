package com.anastasia.Anastasia_BackEnd.core.auth.audit;

public enum PlatformAdminBootstrapAuditOutcome {
    SUCCESS,
    RATE_LIMITED,
    INVALID_SECRET,
    BOOTSTRAP_DISABLED,
    BOOTSTRAP_ALREADY_COMPLETED,
    DUPLICATE_EMAIL,
    FAILED
}
