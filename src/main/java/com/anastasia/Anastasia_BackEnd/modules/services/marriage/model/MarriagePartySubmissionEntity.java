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
@Table(name = "marriage_party_submissions", indexes = {
        @Index(name = "idx_marriage_submission_party_status", columnList = "party_id, status"),
        @Index(name = "idx_marriage_submission_case_version", columnList = "marriage_case_id, submission_version")
})
public class MarriagePartySubmissionEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @Column(name = "marriage_case_id", insertable = false, updatable = false)
    private UUID marriageCaseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private MarriagePartyEntity party;

    @Column(name = "party_id", insertable = false, updatable = false)
    private UUID partyId;

    @Column(name = "submission_version", nullable = false)
    private int submissionVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private MarriagePartySubmissionStatus status;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "return_reason", length = 2000)
    private String returnReason;

    @Column(name = "application_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String applicationSnapshotJson;

    @Version
    @Column(nullable = false)
    private long version;
}
