package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMember;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.ApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "children")
public class Child_MemberEntity extends BaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "child_seq")
    @SequenceGenerator(name = "child_seq", sequenceName = "child_id_seq", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "church_approval_status", nullable = false, length = 24)
    private ApprovalStatus churchApprovalStatus;

    @Column(name = "church_approved_at")
    private LocalDateTime churchApprovedAt;

    @Column(name = "church_approved_by")
    private Long churchApprovedBy;

    @Transient
    private Boolean approvedByChurch;

    @Column(name = "primary_guardian_phone", length = 64)
    private String primaryGuardianPhone;

    @Column(name = "guardian_relationship", length = 64)
    private String guardianRelationship;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "father_id")
    private Adult_MemberEntity father;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mother_id")
    private Adult_MemberEntity mother;

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

    @PrePersist
    public void onChildCreate() {
        if (churchApprovalStatus == null) {
            churchApprovalStatus = ApprovalStatus.PENDING;
        }
    }
}
