package com.anastasia.Anastasia_BackEnd.modules.registration.dto.family;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyMemberSourceType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.RelationshipEndReason;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpsertFamilyRelationshipRequest(
        @NotNull FamilyRelationshipType relationshipType,
        @NotNull FamilyMemberSourceType sourceType,
        Long relatedMemberId,
        Long relatedChildId,
        String displayName,
        Boolean dependent,
        Boolean inHousehold,
        Boolean canManage,
        Boolean primaryGuardian,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        RelationshipEndReason endReason,
        Boolean active,
        Integer sortOrder
) {
}
