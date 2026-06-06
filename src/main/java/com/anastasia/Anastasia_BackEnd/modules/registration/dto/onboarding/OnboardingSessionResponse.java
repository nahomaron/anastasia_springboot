package com.anastasia.Anastasia_BackEnd.modules.registration.dto.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardingSessionResponse {
    UUID sessionId;
    OnboardingSessionStatus status;
    TenantType tenantType;
    SubscriptionPlan selectedPlan;
    String ownerName;
    String ownerEmail;
    String ownerPhone;
    boolean paymentRequired;
    String currency;
    Long expectedAmountMinor;
    String checkoutUrl;
    String checkoutSessionId;
    UUID provisionedTenantId;
    UUID provisionedOwnerUserId;
    String failureReason;
    Instant createdAt;
    Instant updatedAt;
    Instant expiresAt;
    String onboardingAccessToken;
}
