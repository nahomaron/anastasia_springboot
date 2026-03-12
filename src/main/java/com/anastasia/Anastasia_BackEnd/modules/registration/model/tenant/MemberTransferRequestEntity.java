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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
        name = "member_transfer_requests",
        indexes = {
                @Index(name = "idx_member_transfer_user_status", columnList = "user_id, status"),
                @Index(name = "idx_member_transfer_from_tenant_status", columnList = "from_tenant_id, status"),
                @Index(name = "idx_member_transfer_to_tenant_status", columnList = "to_tenant_id, status")
        }
)
public class MemberTransferRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_tenant_id", nullable = false)
    private TenantEntity fromTenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_tenant_id", nullable = false)
    private TenantEntity toTenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MemberTransferStatus status;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @Column(length = 1000)
    private String reason;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        if (this.status == null) {
            this.status = MemberTransferStatus.PENDING;
        }
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
        if (this.statusChangedAt == null) {
            this.statusChangedAt = this.requestedAt;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.statusChangedAt = Instant.now();
        if (this.decidedAt != null && this.executedAt == null && this.status == MemberTransferStatus.APPROVED) {
            this.executedAt = this.decidedAt;
        }
    }
}
