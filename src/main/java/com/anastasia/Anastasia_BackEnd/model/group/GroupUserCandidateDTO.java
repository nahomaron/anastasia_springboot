package com.anastasia.Anastasia_BackEnd.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupUserCandidateDTO {
    private UUID uuid;
    private String fullName;
    private String avatarUrl; // You may already have it in the UserEntity or MembershipEntity
    private boolean alreadyInGroup;
}
