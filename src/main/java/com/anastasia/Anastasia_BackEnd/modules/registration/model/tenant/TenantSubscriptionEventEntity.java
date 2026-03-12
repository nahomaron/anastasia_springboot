package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "tenant_subscription_events",
        indexes = {
                @Index(name = "idx_tenant_subscription_events_tenant_time", columnList = "tenant_id, occurred_at"),
                @Index(name = "idx_tenant_subscription_events_idempotency", columnList = "idempotency_key")
        }
)
public class TenantSubscriptionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_subscription_id", nullable = false)
    private TenantSubscriptionEntity tenantSubscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private TenantSubscriptionEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_plan", length = 32)
    private SubscriptionPlan oldPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_plan", length = 32)
    private SubscriptionPlan newPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 24)
    private SubscriptionStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 24)
    private SubscriptionStatus newStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @PrePersist
    public void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = Instant.now();
        }
    }
}
