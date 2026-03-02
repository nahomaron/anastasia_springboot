package com.anastasia.Anastasia_BackEnd.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDashboardStats {
    private long familyMembers;
    private long upcomingEvents;
    private long sacramentsCompleted;
    private MonthlyOffering monthlyDonations;
}
