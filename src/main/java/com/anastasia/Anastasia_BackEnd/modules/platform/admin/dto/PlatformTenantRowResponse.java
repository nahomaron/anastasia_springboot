package com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class PlatformTenantRowResponse {
    private UUID tenantId;
    private String name;
    private SubscriptionPlan plan;
    private TenantStatus status;
    private SubscriptionStatus billingStatus;
    private long activeMembers;
    private long priests;
    private Instant renewalDate;
    private String contactEmail;
    private String accountOwner;
    private String region;
    private Instant createdAt;
    private String notes;
}
