package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingOverrideType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantBillingChargeSummaryResponse {
    private long normalAmountMinor;
    private long discountAmountMinor;
    private long effectiveAmountMinor;
    private String currency;
    private UUID appliedOverrideId;
    private BillingOverrideType appliedBillingOverrideType;
    private Instant overrideEndsAt;
}
