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

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "marriage_confessor_approvals", indexes = {
        @Index(name = "idx_marriage_confessor_case_status", columnList = "marriage_case_id, approval_status"),
        @Index(name = "idx_marriage_confessor_mode", columnList = "approval_mode, priest_user_id")
})
public class MarriageConfessorApprovalEntity extends Auditable {

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
    @Column(name = "approval_status", nullable = false, length = 24)
    private MarriageConfessorApprovalStatus approvalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 24)
    private MarriageConfessorApprovalMode approvalMode;

    @Column(name = "priest_user_id")
    private UUID priestUserId;

    @Column(name = "priest_person_name", length = 255)
    private String priestPersonName;

    @Column(name = "church_name", length = 255)
    private String churchName;

    @Column(name = "diocese_name", length = 255)
    private String dioceseName;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "evidence_document_id")
    private UUID evidenceDocumentId;

    @Column(name = "notes", nullable = false, length = 2000)
    private String notes;

    @Column(name = "blocking", nullable = false)
    @Builder.Default
    private boolean blocking = true;

    @Column(name = "override_reason", length = 2000)
    private String overrideReason;

    @Column(name = "override_document_id")
    private UUID overrideDocumentId;

    @Version
    @Column(nullable = false)
    private long version;
}
