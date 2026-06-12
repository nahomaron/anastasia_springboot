package com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantEmailSettingsResponse {
    private boolean quotaEnforced;
    private boolean sendingSuspended;
    private String suspensionReason;
    private Integer monthlyQuota;
    private Integer effectiveMonthlyQuota;
    private long currentPeriodSentCount;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
}
