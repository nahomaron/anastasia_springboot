package com.anastasia.Anastasia_BackEnd.modules.groups.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchInviteResponse{
    private String groupName;
    private int invitedCount;
    private int skippedCount;
    private int notFoundCount;
    private List<UUID> invitedUserIds;
    private List<String> skippedEmails;
    private List<String> notFoundEmails;
}
