package com.anastasia.Anastasia_BackEnd.modules.accounting.model;

import com.anastasia.Anastasia_BackEnd.modules.common.AuditMetadata;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class for all entities to provide common auditing fields.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity extends AuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
