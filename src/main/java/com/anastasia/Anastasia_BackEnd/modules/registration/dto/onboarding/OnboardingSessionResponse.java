package com.anastasia.Anastasia_BackEnd.modules.registration.dto.onboarding;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class OnboardingSessionResponse {
    UUID sessionId;
    OnboardingSessionStatus status;
    TenantType tenantType;
    SubscriptionPlan selectedPlan;
    boolean paymentRequired;
    String currency;
    Long expectedAmountMinor;
    String checkoutUrl;
    String checkoutSessionId;
    UUID provisionedTenantId;
    UUID provisionedOwnerUserId;
    String failureReason;
    LocalDateTime expiresAt;
}
