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
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "marriage_certificates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_marriage_certificate_number", columnNames = {"certificate_number"})
        },
        indexes = {
                @Index(name = "idx_marriage_certificate_case_status", columnList = "marriage_case_id, status"),
                @Index(name = "idx_marriage_certificate_issued_date", columnList = "issued_date")
        }
)
public class MarriageCertificateEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @Column(name = "certificate_number", nullable = false, length = 64)
    private String certificateNumber;

    @Column(name = "numbering_format_snapshot", nullable = false, length = 255)
    private String numberingFormatSnapshot;

    @Column(name = "issued_date", nullable = false)
    private Instant issuedDate;

    @Column(name = "issued_by_user_id", nullable = false)
    private UUID issuedByUserId;

    @Column(name = "locked_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String lockedSnapshotJson;

    @Column(name = "print_count", nullable = false)
    @Builder.Default
    private int printCount = 0;

    @Column(name = "registry_reference", length = 128)
    private String registryReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private MarriageCertificateStatus status = MarriageCertificateStatus.DRAFT;

    @Column(name = "has_amendment", nullable = false)
    @Builder.Default
    private boolean hasAmendment = false;

    @Version
    @Column(nullable = false)
    private long version;
}
