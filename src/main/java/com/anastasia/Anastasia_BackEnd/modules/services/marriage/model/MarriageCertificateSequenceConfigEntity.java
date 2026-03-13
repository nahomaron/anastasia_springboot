package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "marriage_certificate_sequence_configs", indexes = {
        @Index(name = "idx_marriage_certificate_sequence_church", columnList = "church_id, active")
})
public class MarriageCertificateSequenceConfigEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    @Column(name = "prefix", length = 32)
    private String prefix;

    @Column(name = "separator", length = 8)
    private String separator;

    @Column(name = "current_number", nullable = false)
    private long currentNumber;

    @Column(name = "starting_seed", nullable = false)
    private long startingSeed;

    @Column(name = "reset_mode", length = 32)
    private String resetMode;

    @Column(name = "format_mask", nullable = false, length = 128)
    private String formatMask;

    @Column(name = "migration_reference", length = 128)
    private String migrationReference;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;
}
