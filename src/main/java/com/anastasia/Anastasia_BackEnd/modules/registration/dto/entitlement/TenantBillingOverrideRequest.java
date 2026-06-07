package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingOverrideType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantBillingOverrideRequest {

    @NotNull
    private BillingOverrideType overrideType;

    private Instant startsAt;

    private Instant endsAt;

    private BigDecimal discountPercent;

    private Long fixedAmountMinor;

    private String currency;

    private String reason;

    private String internalNote;
}
