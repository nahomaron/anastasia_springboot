package com.anastasia.Anastasia_BackEnd.modules.services.marriage.model;

import com.anastasia.Anastasia_BackEnd.modules.common.Auditable;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
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
import java.time.LocalDate;
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
@Table(
        name = "marriage_parties",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_marriage_case_party_role", columnNames = {"marriage_case_id", "party_role"})
        },
        indexes = {
                @Index(name = "idx_marriage_party_case_role", columnList = "marriage_case_id, party_role"),
                @Index(name = "idx_marriage_party_member", columnList = "member_id")
        }
)
public class MarriagePartyEntity extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marriage_case_id", nullable = false)
    private MarriageCaseEntity marriageCase;

    @Column(name = "marriage_case_id", insertable = false, updatable = false)
    private UUID marriageCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "party_role", nullable = false, length = 16)
    private MarriagePartyRole partyRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Adult_MemberEntity member;

    @Column(name = "member_id", insertable = false, updatable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id")
    private UserEntity linkedUser;

    @Column(name = "linked_user_id", insertable = false, updatable = false)
    private UUID linkedUserId;

    @Column(name = "external_applicant", nullable = false)
    @Builder.Default
    private boolean externalApplicant = false;

    @Column(name = "counterpart_placeholder", nullable = false)
    @Builder.Default
    private boolean counterpartPlaceholder = false;

    @Column(name = "submitted", nullable = false)
    @Builder.Default
    private boolean submitted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "latest_submission_status", nullable = false, length = 32)
    @Builder.Default
    private MarriagePartySubmissionStatus latestSubmissionStatus = MarriagePartySubmissionStatus.DRAFT;

    @Column(name = "editable", nullable = false)
    @Builder.Default
    private boolean editable = true;

    @Column(name = "full_legal_name_english", length = 255)
    private String fullLegalNameEnglish;

    @Column(name = "full_legal_name_local", length = 255)
    private String fullLegalNameLocal;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "marital_status", length = 64)
    private String maritalStatus;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "phone", column = @Column(name = "contact_phone", length = 64)),
            @AttributeOverride(name = "alternatePhone", column = @Column(name = "contact_alternate_phone", length = 64)),
            @AttributeOverride(name = "email", column = @Column(name = "contact_email", length = 255)),
            @AttributeOverride(name = "addressLine", column = @Column(name = "contact_address_line", length = 512)),
            @AttributeOverride(name = "currentCountry", column = @Column(name = "contact_current_country", length = 128)),
            @AttributeOverride(name = "currentCity", column = @Column(name = "contact_current_city", length = 128))
    })
    private MarriageContactInfo contactInfo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "governmentIdType", column = @Column(name = "identity_government_id_type", length = 64)),
            @AttributeOverride(name = "governmentIdNumber", column = @Column(name = "identity_government_id_number", length = 128)),
            @AttributeOverride(name = "passportNumber", column = @Column(name = "identity_passport_number", length = 128)),
            @AttributeOverride(name = "documentNumber", column = @Column(name = "identity_document_number", length = 128)),
            @AttributeOverride(name = "documentExpiryDate", column = @Column(name = "identity_document_expiry_date"))
    })
    private MarriageIdentityInfo identityInfo;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Builder.Default
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MarriagePartySubmissionEntity> submissions = new HashSet<>();

    @Version
    @Column(nullable = false)
    private long version;
}
