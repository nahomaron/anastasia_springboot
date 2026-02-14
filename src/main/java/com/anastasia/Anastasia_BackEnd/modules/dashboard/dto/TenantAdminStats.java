package com.anastasia.Anastasia_BackEnd.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAdminStats {
    private long activeMembers;
    private long children;
    private long priestsStaff;
    private MonthlyOffering monthlyOffering;
}
