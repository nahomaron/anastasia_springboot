package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
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
@Table(name = "marriage_certificate_amendments", indexes = {
        @Index(name = "idx_marriage_certificate_amendment_certificate", columnList = "certificate_id, amended_at")
})
public class MarriageCertificateAmendmentEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private MarriageCertificateEntity certificate;

    @Column(name = "amendment_reason", nullable = false, length = 2000)
    private String amendmentReason;

    @Column(name = "amendment_snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String amendmentSnapshotJson;

    @Column(name = "amended_by_user_id", nullable = false)
    private UUID amendedByUserId;

    @Column(name = "amended_at", nullable = false)
    private Instant amendedAt;
}
