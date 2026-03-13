package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "marriage_cases", indexes = {
        @Index(name = "idx_marriage_case_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_marriage_case_church_status", columnList = "church_id, status"),
        @Index(name = "idx_marriage_case_church_reference", columnList = "church_id, case_reference")
})
public class MarriageCaseEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    @Column(name = "church_id", insertable = false, updatable = false)
    private Long churchId;

    @Column(name = "case_reference", nullable = false, unique = true, length = 64)
    private String caseReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    @Builder.Default
    private MarriageCaseStatus status = MarriageCaseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 32)
    private MarriageCaseOriginType originType;

    @Enumerated(EnumType.STRING)
    @Column(name = "pairing_mode", nullable = false, length = 32)
    private MarriagePairingMode pairingMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_language", nullable = false, length = 8)
    @Builder.Default
    private MarriageLanguageCode primaryLanguage = MarriageLanguageCode.EN;

    @Column(name = "bride_party_id")
    private UUID bridePartyId;

    @Column(name = "groom_party_id")
    private UUID groomPartyId;

    @Column(name = "both_submitted", nullable = false)
    @Builder.Default
    private boolean bothSubmitted = false;

    @Column(name = "secretary_clearance_complete", nullable = false)
    @Builder.Default
    private boolean secretaryClearanceComplete = false;

    @Column(name = "admin_approval_granted", nullable = false)
    @Builder.Default
    private boolean adminApprovalGranted = false;

    @Column(name = "confessor_gate_satisfied", nullable = false)
    @Builder.Default
    private boolean confessorGateSatisfied = false;

    @Column(name = "manual_payment_satisfied", nullable = false)
    @Builder.Default
    private boolean manualPaymentSatisfied = false;

    @Column(name = "ready_for_scheduling", nullable = false)
    @Builder.Default
    private boolean readyForScheduling = false;

    @Column(name = "ceremony_completed", nullable = false)
    @Builder.Default
    private boolean ceremonyCompleted = false;

    @Column(name = "certificate_issued", nullable = false)
    @Builder.Default
    private boolean certificateIssued = false;

    @Column(name = "archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Builder.Default
    @OneToMany(mappedBy = "marriageCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MarriagePartyEntity> parties = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "marriageCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MarriageStatusHistoryEntity> statusHistory = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;
}
