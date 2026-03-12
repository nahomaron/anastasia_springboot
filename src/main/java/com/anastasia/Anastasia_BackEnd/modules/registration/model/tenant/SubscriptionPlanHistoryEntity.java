package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "tenant_subscription_plan_history",
        indexes = {
                @Index(name = "idx_tenant_subscription_plan_history_tenant_effective", columnList = "tenant_id, effective_at"),
                @Index(name = "idx_tenant_subscription_plan_history_event", columnList = "provider_event_id")
        }
)
public class SubscriptionPlanHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "tenant_subscription_id")
    private UUID tenantSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_plan", length = 32)
    private SubscriptionPlan oldPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_plan", nullable = false, length = 32)
    private SubscriptionPlan newPlan;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;

    @Column(length = 512)
    private String reason;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "provider_event_id", length = 128)
    private String providerEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (this.effectiveAt == null) {
            this.effectiveAt = LocalDateTime.now();
        }
        this.createdAt = LocalDateTime.now();
    }
}
