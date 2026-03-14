package com.anastasia.Anastasia_BackEnd.modules.common;

import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transitional audit base for entities that historically exposed
 * {@code createdDate}/{@code lastModifiedDate} style accessors.
 * <p>
 * The canonical persisted fields still live in {@link AuditMetadata}. The
 * legacy accessors below are compatibility shims and should not be used in new
 * repository or domain code.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@MappedSuperclass
public abstract class Auditable extends AuditMetadata {

    protected Auditable() {
        super();
    }

    @Deprecated(forRemoval = true)
    public LocalDateTime getCreatedDate() {
        return toLocalDateTime(getCreatedAt());
    }

    @Deprecated(forRemoval = true)
    public void setCreatedDate(LocalDateTime createdDate) {
        setCreatedAt(toInstant(createdDate));
    }

    @Deprecated(forRemoval = true)
    public LocalDateTime getLastModifiedDate() {
        return toLocalDateTime(getUpdatedAt());
    }

    @Deprecated(forRemoval = true)
    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        setUpdatedAt(toInstant(lastModifiedDate));
    }

    @Deprecated(forRemoval = true)
    public UUID getLastModifiedBy() {
        return getUpdatedBy();
    }

    @Deprecated(forRemoval = true)
    public void setLastModifiedBy(UUID lastModifiedBy) {
        setUpdatedBy(lastModifiedBy);
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, java.time.ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(java.time.ZoneOffset.UTC);
    }
}
