package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipSummary {
    private String memberId;
    private String fullName;
    private String relationshipToUser;
    private String status;
    private String churchName;
    private boolean isPrimaryGuardian;
}
