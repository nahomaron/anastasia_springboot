package com.anastasia.Anastasia_BackEnd.modules.platform.admin.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "support_access_sessions", indexes = {
        @Index(name = "idx_support_access_sessions_tenant_time", columnList = "tenant_id, created_at"),
        @Index(name = "idx_support_access_sessions_actor_status", columnList = "actor_user_id, status")
})
public class SupportAccessSessionEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private UserEntity actor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private SupportAccessScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SupportAccessSessionStatus status;

    @Column(name = "denial_reason", length = 512)
    private String denialReason;

    @Column(name = "end_reason", length = 512)
    private String endReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @OneToMany(mappedBy = "session")
    @Default
    private Set<SupportAccessActionEntity> actions = new LinkedHashSet<>();

    @Version
    @Column(nullable = false)
    private long version;
}
