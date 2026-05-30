package com.anastasia.Anastasia_BackEnd.modules.platform.admin.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "platform_admin_recovery_audit_events")
public class PlatformAdminRecoveryAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempted_email", length = 320)
    private String attemptedEmail;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "issued_token_id")
    private Integer issuedTokenId;

    @Column(name = "operator_name", length = 120)
    private String operatorName;

    @Column(length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private PlatformAdminRecoveryAuditOutcome outcome;

    @Column(length = 512)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}
