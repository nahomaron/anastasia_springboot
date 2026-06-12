package com.anastasia.Anastasia_BackEnd.modules.platform.admin.model;

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
@Table(name = "support_access_actions", indexes = {
        @Index(name = "idx_support_access_actions_session_time", columnList = "support_access_session_id, occurred_at"),
        @Index(name = "idx_support_access_actions_tenant_time", columnList = "tenant_id, occurred_at")
})
public class SupportAccessActionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "support_access_session_id", nullable = false)
    private SupportAccessSessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private UserEntity actor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private SupportAccessActionType actionType;

    @Column(name = "http_method", nullable = false, length = 16)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 512)
    private String requestPath;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private SupportAccessActionOutcome outcome;

    @Column(name = "detail", length = 1024)
    private String detail;

    @Builder.Default
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}
