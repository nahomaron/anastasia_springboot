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
@Table(name = "marriage_reviews", indexes = {
        @Index(name = "idx_marriage_review_case_stage", columnList = "marriage_case_id, stage"),
        @Index(name = "idx_marriage_review_actor", columnList = "actor_user_id, stage")
})
public class MarriageReviewEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 48)
    private MarriageReviewStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private MarriageReviewDecision decision;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "actor_role", nullable = false, length = 64)
    private String actorRole;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Column(name = "notes", length = 4000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 32)
    private MarriageNoteVisibility visibility;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;
}
