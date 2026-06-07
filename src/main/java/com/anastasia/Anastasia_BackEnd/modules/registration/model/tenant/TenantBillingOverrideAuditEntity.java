package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tenant_billing_override_audit")
public class TenantBillingOverrideAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(name = "billing_override_id", nullable = false)
    private UUID billingOverrideId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 24)
    private TenantBillingOverrideAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", length = 32)
    private BillingOverrideType overrideType;

    @Column(name = "old_value_summary", length = 2048)
    private String oldValueSummary;

    @Column(name = "new_value_summary", length = 2048)
    private String newValueSummary;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    public void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
