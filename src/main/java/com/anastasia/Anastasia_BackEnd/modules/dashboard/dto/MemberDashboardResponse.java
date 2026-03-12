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
public class MemberDashboardResponse {
    private MemberDashboardStats stats;
    private String churchDisplayName;
    private List<MemberFamilyItem> familyMembers;
    private List<MemberUpcomingEventItem> upcomingEvents;
}
