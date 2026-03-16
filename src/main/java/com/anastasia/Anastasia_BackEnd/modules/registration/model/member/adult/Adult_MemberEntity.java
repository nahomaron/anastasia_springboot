package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import jakarta.persistence.AttributeOverride;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@AttributeOverride(name = "phone", column = @Column(nullable = false))
@Table(name = "members", indexes = {
        @Index(name = "idx_member_church", columnList = "church_id"),
        @Index(name = "idx_member_tenant", columnList = "tenant_id")
})
//@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
//@Filters(@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId"))
public class Adult_MemberEntity extends BaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(name = "member_seq", sequenceName = "member_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "church_approval_status", nullable = false, length = 24)
    private ApprovalStatus churchApprovalStatus = ApprovalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "priest_approval_status", nullable = false, length = 24)
    private ApprovalStatus priestApprovalStatus = ApprovalStatus.PENDING;

    @Column(name = "church_approved_at")
    private LocalDateTime churchApprovedAt;

    @Column(name = "church_approved_by")
    private Long churchApprovedBy;

    @Column(name = "priest_approved_at")
    private LocalDateTime priestApprovedAt;

    @Column(name = "priest_approved_by")
    private Long priestApprovedBy;

    @Transient
    private Boolean approvedByChurch;

    @Transient
    private Boolean approvedByPriest;

    @Column(name = "terms_accepted", nullable = false)
    private boolean termsAccepted;

    @Column(name = "terms_version", nullable = false)
    private String termsVersion;

    @Column(name = "terms_accepted_at", nullable = false)
    private Instant termsAcceptedAt;

    private String eritreaContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MaritalStatus maritalStatus;

    @Transient
    private Integer numberOfChildren;

    private String profession;

    private String spouseIdNumber;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "father", fetch = FetchType.LAZY)
    private Set<Child_MemberEntity> childrenAsFather = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @Builder.Default
    @OneToMany(mappedBy = "mother", fetch = FetchType.LAZY)
    private Set<Child_MemberEntity> childrenAsMother = new HashSet<>();

    public boolean isApprovedByChurch() {
        return approvedByChurch != null ? approvedByChurch : churchApprovalStatus == ApprovalStatus.APPROVED;
    }

    public void setApprovedByChurch(boolean approvedByChurch) {
        this.approvedByChurch = approvedByChurch;
        this.churchApprovalStatus = approvedByChurch ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;
        if (approvedByChurch && churchApprovedAt == null) {
            churchApprovedAt = LocalDateTime.now();
        }
        if (!approvedByChurch) {
            churchApprovedAt = null;
            churchApprovedBy = null;
        }
    }

    public boolean isApprovedByPriest() {
        return approvedByPriest != null ? approvedByPriest : priestApprovalStatus == ApprovalStatus.APPROVED;
    }

    public void setApprovedByPriest(boolean approvedByPriest) {
        this.approvedByPriest = approvedByPriest;
        this.priestApprovalStatus = approvedByPriest ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;
        if (approvedByPriest && priestApprovedAt == null) {
            priestApprovedAt = LocalDateTime.now();
        }
        if (!approvedByPriest) {
            priestApprovedAt = null;
            priestApprovedBy = null;
        }
    }

    @Transient
    public int getNumberOfChildren() {
        if (numberOfChildren != null) {
            return numberOfChildren;
        }
        int fatherCount = childrenAsFather == null ? 0 : childrenAsFather.size();
        int motherCount = childrenAsMother == null ? 0 : childrenAsMother.size();
        return Math.max(fatherCount, motherCount);
    }

    public void setNumberOfChildren(int numberOfChildren) {
        this.numberOfChildren = numberOfChildren;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public String getTermsVersion() {
        return termsVersion;
    }

    public void setTermsAcceptedAt(Instant termsAcceptedAt) {
        this.termsAcceptedAt = termsAcceptedAt;
        this.setConsentAcceptedAt(termsAcceptedAt != null ? LocalDateTime.ofInstant(termsAcceptedAt, java.time.ZoneOffset.UTC) : null);
    }

    public Instant getTermsAcceptedAt() {
        return termsAcceptedAt;
    }

    public void setTermsVersion(String termsVersion) {
        this.termsVersion = termsVersion;
        this.setConsentVersion(termsVersion);
    }

    @PrePersist
    public void onAdultMemberCreate() {
        if (isTermsAccepted() && getConsentAcceptedAt() == null && termsAcceptedAt != null) {
            setConsentAcceptedAt(LocalDateTime.ofInstant(termsAcceptedAt, java.time.ZoneOffset.UTC));
        }
        if (getConsentVersion() == null && termsVersion != null) {
            setConsentVersion(termsVersion);
        }
    }
}
