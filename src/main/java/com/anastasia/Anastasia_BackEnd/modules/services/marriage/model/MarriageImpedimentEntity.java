package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
@Table(name = "marriage_impediments", indexes = {
        @Index(name = "idx_marriage_impediment_case_status", columnList = "marriage_case_id, status"),
        @Index(name = "idx_marriage_impediment_party_status", columnList = "party_id, status")
})
public class MarriageImpedimentEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private MarriagePartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(name = "impediment_type", nullable = false, length = 48)
    private MarriageImpedimentType impedimentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private MarriageImpedimentSeverity severity;

    @Column(name = "source_stage", nullable = false, length = 64)
    private String sourceStage;

    @Column(name = "blocking", nullable = false)
    @Builder.Default
    private boolean blocking = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private MarriageImpedimentStatus status = MarriageImpedimentStatus.OPEN;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    @Column(name = "evidence_note", length = 2000)
    private String evidenceNote;

    @Version
    @Column(nullable = false)
    private long version;
}
