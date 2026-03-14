package com.anastasia.Anastasia_BackEnd.modules.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Lightweight timestamp-only audit base for entities that manage lifecycle
 * timestamps explicitly instead of using Spring Data auditing callbacks.
 * <p>
 * Even here, the canonical persisted property names remain {@code createdAt}
 * and {@code updatedAt}.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class LocalDateTimeAuditMetadata {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected void initializeAuditTimestamps(Instant now) {
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    protected void touchAuditTimestamps(Instant now) {
        updatedAt = now;
    }
}
