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

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "marriage_requirement_assignments", indexes = {
        @Index(name = "idx_marriage_requirement_assignment_case", columnList = "marriage_case_id, current_status"),
        @Index(name = "idx_marriage_requirement_assignment_party", columnList = "party_id, current_status")
})
public class MarriageRequirementAssignmentEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_template_id", nullable = false)
    private MarriageRequirementTemplateEntity requirementTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private MarriagePartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 24)
    @Builder.Default
    private MarriageRequirementStatus currentStatus = MarriageRequirementStatus.PENDING;

    @Column(name = "satisfied_by_user_id")
    private UUID satisfiedByUserId;

    @Column(name = "satisfied_at")
    private Instant satisfiedAt;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "blocking", nullable = false)
    @Builder.Default
    private boolean blocking = true;

    @Version
    @Column(nullable = false)
    private long version;
}
