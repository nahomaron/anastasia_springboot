package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMembershipsResponse {
    private MembershipSummary selfMembership;
    private List<MembershipSummary> managedMemberships;
}
