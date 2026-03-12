package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMemberSummaryResponse {
    private Long relationshipId;
    private String id;
    private FamilyMemberSourceType sourceType;
    private Long sourceId;
    private String fullName;
    private String relationship;
    private String membershipStatus;
    private boolean canManage;
    private boolean primaryGuardian;
    private boolean accountHolder;
    private boolean dependent;
    private boolean inHousehold;
    private boolean linkedToMemberProfile;
    private String membershipNumber;
    private Integer sortOrder;
    private String maritalStatus;
    private boolean active;
}
