package com.anastasia.Anastasia_BackEnd.modules.common;

import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@MappedSuperclass
public abstract class Auditable extends AuditMetadata {

    protected Auditable() {
        super();
    }

    public LocalDateTime getCreatedDate() {
        return toLocalDateTime(getCreatedAt());
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        setCreatedAt(toInstant(createdDate));
    }

    public LocalDateTime getLastModifiedDate() {
        return toLocalDateTime(getUpdatedAt());
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        setUpdatedAt(toInstant(lastModifiedDate));
    }

    public UUID getLastModifiedBy() {
        return getUpdatedBy();
    }

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
