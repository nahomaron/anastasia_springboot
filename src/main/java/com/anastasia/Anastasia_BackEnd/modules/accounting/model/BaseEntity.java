package com.anastasia.Anastasia_BackEnd.modules.accounting.model;

import com.anastasia.Anastasia_BackEnd.modules.common.AuditMetadata;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Accounting base entity with a generated numeric identifier and the canonical
 * shared audit fields from {@link AuditMetadata}.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity extends AuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
