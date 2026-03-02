package com.anastasia.Anastasia_BackEnd.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriestDashboardResponse {
    private PriestDashboardStats stats;
    private List<MemberOverviewItem> recentMembers;
    private List<PriestRequestItem> recentRequests;
}
