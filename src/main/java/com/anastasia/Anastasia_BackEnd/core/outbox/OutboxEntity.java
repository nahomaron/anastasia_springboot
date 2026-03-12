package com.anastasia.Anastasia_BackEnd.core.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * OutboxEntity represents an event stored in the outbox table for reliable event publishing.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_next_attempt_created", columnList = "status,next_attempt_at,created_at"),
        @Index(name = "idx_outbox_tenant_type_created", columnList = "tenant_id,type,created_at"),
        @Index(name = "idx_outbox_correlation", columnList = "correlation_id")
})
@Getter
@Setter
public class OutboxEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 128)
    private String aggregateType;

    @Column(length = 128)
    private String aggregateId;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 128)
    private String type;

    @Column(name = "user_email")
    private String userEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode headers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "correlation_id", length = 160)
    private String correlationId;

    @Column(name = "causation_id", length = 160)
    private String causationId;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_error", length = 1024)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
    }

    public boolean isPublished() {
        return status == OutboxStatus.PUBLISHED;
    }

    public void setPublished(boolean published) {
        status = published ? OutboxStatus.PUBLISHED : OutboxStatus.PENDING;
        if (published) {
            publishedAt = publishedAt == null ? Instant.now() : publishedAt;
            failedAt = null;
            lastError = null;
            nextAttemptAt = null;
        } else if (status == OutboxStatus.PENDING) {
            publishedAt = null;
        }
    }
}
