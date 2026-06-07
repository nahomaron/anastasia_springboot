package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingOverrideType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantBillingOverrideResponse {
    private UUID id;
    private UUID tenantId;
    private BillingOverrideType overrideType;
    private boolean active;
    private boolean effective;
    private Instant startsAt;
    private Instant endsAt;
    private BigDecimal discountPercent;
    private Long fixedAmountMinor;
    private String currency;
    private String reason;
    private String internalNote;
    private UUID createdByUserId;
    private UUID updatedByUserId;
    private UUID revokedByUserId;
    private Instant revokedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
