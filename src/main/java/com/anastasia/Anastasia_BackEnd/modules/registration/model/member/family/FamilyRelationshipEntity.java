package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family;

import com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "family_relationships", indexes = {
        @Index(name = "idx_family_relationships_tenant_owner", columnList = "tenant_id, owner_member_id"),
        @Index(name = "idx_family_relationships_related_member", columnList = "related_member_id"),
        @Index(name = "idx_family_relationships_related_child", columnList = "related_child_id")
})
public class FamilyRelationshipEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_member_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Adult_MemberEntity ownerMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 32)
    private FamilyRelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private FamilyMemberSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_member_id")
    @EqualsAndHashCode.Exclude
    private Adult_MemberEntity relatedMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_child_id")
    @EqualsAndHashCode.Exclude
    private Child_MemberEntity relatedChild;

    @Column(name = "display_name")
    private String displayName;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", length = 32)
    private RelationshipEndReason endReason;

    @Column(nullable = false)
    private boolean dependent;

    @Column(name = "in_household", nullable = false)
    private boolean inHousehold;

    @Column(name = "can_manage", nullable = false)
    private boolean canManage;

    @Builder.Default
    @Column(name = "is_primary_guardian", nullable = false)
    private boolean primaryGuardian = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (effectiveFrom == null) {
            effectiveFrom = now;
        }
        initializeAuditTimestamps(now);
    }

    @PreUpdate
    public void onUpdate() {
        touchAuditTimestamps(LocalDateTime.now());
    }
}
