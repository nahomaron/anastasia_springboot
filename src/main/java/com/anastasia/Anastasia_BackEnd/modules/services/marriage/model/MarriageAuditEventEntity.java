package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "marriage_audit_events", indexes = {
        @Index(name = "idx_marriage_audit_case_type", columnList = "marriage_case_id, event_type"),
        @Index(name = "idx_marriage_audit_occurred_at", columnList = "occurred_at")
})
public class MarriageAuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private MarriageCaseAuditEventType eventType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "related_party_id")
    private UUID relatedPartyId;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
