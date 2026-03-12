package com.anastasia.Anastasia_BackEnd.modules.services.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "baptism_service_requests",
        indexes = {
                @Index(name = "idx_baptism_requests_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_baptism_requests_church_status", columnList = "church_id, status"),
                @Index(name = "idx_baptism_requests_requested_by", columnList = "requested_by_user_id")
        }
)
public class BaptismRequestEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "baptism_request_seq")
    @SequenceGenerator(name = "baptism_request_seq", sequenceName = "baptism_request_seq", allocationSize = 1)
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 32)
    private String requestNumber;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "church_id", nullable = false)
    private ChurchEntity church;

    @Column(name = "church_number", nullable = false, length = 32)
    private String churchNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private UserEntity requestedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    @Builder.Default
    private BaptismRequestStatus status = BaptismRequestStatus.PENDING;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "baptism_date", nullable = false)
    private LocalDate baptismDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName", column = @Column(name = "local_full_name", nullable = false)),
            @AttributeOverride(name = "baptismalName", column = @Column(name = "local_baptismal_name", nullable = false)),
            @AttributeOverride(name = "fatherFullName", column = @Column(name = "local_father_full_name", nullable = false)),
            @AttributeOverride(name = "motherFullName", column = @Column(name = "local_mother_full_name", nullable = false)),
            @AttributeOverride(name = "godParentFullName", column = @Column(name = "local_god_parent_full_name", nullable = false)),
            @AttributeOverride(name = "priestFullName", column = @Column(name = "local_priest_full_name", nullable = false)),
            @AttributeOverride(name = "churchOfBaptismName", column = @Column(name = "local_church_of_baptism_name", nullable = false))
    })
    private BaptismLanguageDetails localLanguage;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName", column = @Column(name = "english_full_name", nullable = false)),
            @AttributeOverride(name = "baptismalName", column = @Column(name = "english_baptismal_name", nullable = false)),
            @AttributeOverride(name = "fatherFullName", column = @Column(name = "english_father_full_name", nullable = false)),
            @AttributeOverride(name = "motherFullName", column = @Column(name = "english_mother_full_name", nullable = false)),
            @AttributeOverride(name = "godParentFullName", column = @Column(name = "english_god_parent_full_name", nullable = false)),
            @AttributeOverride(name = "priestFullName", column = @Column(name = "english_priest_full_name", nullable = false)),
            @AttributeOverride(name = "churchOfBaptismName", column = @Column(name = "english_church_of_baptism_name", nullable = false))
    })
    private BaptismLanguageDetails english;

    @Column(name = "baby_photo_url", nullable = false, length = 1024)
    private String babyPhotoUrl;

    @Column(name = "baby_photo_size", length = 64)
    private String babyPhotoSize;

    @Column(name = "birth_certificate_url", nullable = false, length = 1024)
    private String birthCertificateUrl;

    @Column(name = "birth_certificate_size", length = 64)
    private String birthCertificateSize;

    @Column(name = "father_signature_url", nullable = false, length = 1024)
    private String fatherSignatureUrl;

    @Column(name = "father_signature_size", length = 64)
    private String fatherSignatureSize;

    @Column(name = "priest_signature_url", nullable = false, length = 1024)
    private String priestSignatureUrl;

    @Column(name = "priest_signature_size", length = 64)
    private String priestSignatureSize;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "review_decision_note", length = 2000)
    private String reviewDecisionNote;

    @Column(name = "reviewer_role", length = 64)
    private String reviewerRole;

    @Version
    @Column(nullable = false)
    private long version;
}
