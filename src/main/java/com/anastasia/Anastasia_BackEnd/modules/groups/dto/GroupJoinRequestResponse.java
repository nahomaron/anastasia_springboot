package com.anastasia.Anastasia_BackEnd.modules.groups.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupJoinRequestResponse {
    private Long id;
    private Long groupId;
    private UUID requesterId;
    private String requesterName;
    private String requesterEmail;
    private String status;
    private String decisionNote;
    private Instant requestedAt;
    private Instant decidedAt;
    private UUID decidedBy;
}
