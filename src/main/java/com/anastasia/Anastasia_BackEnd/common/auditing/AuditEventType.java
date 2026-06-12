package com.anastasia.Anastasia_BackEnd.common.auditing;

/**
 * Shared audit taxonomy for high-risk cross-module events written into
 * {@code audit_logs}. Event names stay action-oriented and stable so support,
 * compliance, and downstream tooling can query them consistently.
 *
 * Specialized audit tables remain authoritative for module-specific histories
 * such as marriage workflow, membership-card activity, entitlement changes,
 * and billing overrides. Where those tables already exist, their event names
 * should align conceptually with this taxonomy instead of duplicating every
 * row in the shared table.
 */
public enum AuditEventType {
    AUTH_LOGIN_SUCCEEDED,
    AUTH_LOGIN_FAILED,
    AUTH_MFA_CHALLENGE_ISSUED,
    AUTH_MFA_VERIFIED,
    AUTH_PASSWORD_RESET_REQUESTED,
    AUTH_PASSWORD_RESET_COMPLETED,
    ACCESS_ROLE_CHANGED,
    ACCESS_PERMISSION_CHANGED,
    TENANT_SETTINGS_CHANGED,
    BILLING_SUBSCRIPTION_CHANGED,
    DATA_EXPORT_REQUESTED,
    DATA_DELETION_EXECUTED,
    DATA_PURGE_EXECUTED,
    MEMBERSHIP_VERIFICATION_PERFORMED,
    SUPPORT_ACCESS_GRANTED,
    PUBLIC_DIRECTORY_VISIBILITY_CHANGED,
    SENSITIVE_DOCUMENT_DOWNLOADED,
    SENSITIVE_DOCUMENT_UPLOADED
}
