package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card;

/**
 * Membership-card specific audit events. These remain in the specialized card
 * audit table and align conceptually with the shared taxonomy:
 * VERIFIED -> MEMBERSHIP_VERIFICATION_PERFORMED
 * DOWNLOADED -> SENSITIVE_DOCUMENT_DOWNLOADED
 */
public enum MembershipCardAuditEventType {
    ISSUED,
    DOWNLOADED,
    VERIFIED,
    REVOKED,
    TEMPLATE_CHANGED
}
