package com.anastasia.Anastasia_BackEnd.core.auth.audit;

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
@Table(name = "platform_admin_bootstrap_audit_events")
public class PlatformAdminBootstrapAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempted_email", length = 320)
    private String attemptedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private PlatformAdminBootstrapAuditOutcome outcome;

    @Column(length = 512)
    private String detail;

    @Column(name = "ip_address", length = 128)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_user_id")
    private UUID createdUserId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}
