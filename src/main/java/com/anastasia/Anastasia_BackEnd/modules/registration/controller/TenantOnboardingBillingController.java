package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.onboarding.OnboardingSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingBillingService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/onboarding/billing")
public class TenantOnboardingBillingController {

    private final TenantOnboardingBillingService onboardingBillingService;
    private final StripeReadinessService stripeReadinessService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/sessions")
    public ResponseEntity<OnboardingSessionResponse> createOnboardingSession(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody TenantDTO tenantDTO
    ) {
        OnboardingSessionResponse response = onboardingBillingService.createSession(tenantDTO, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/sessions/{sessionId}/checkout")
    public ResponseEntity<OnboardingSessionResponse> createCheckout(
            @PathVariable UUID sessionId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey
    ) {
        OnboardingSessionResponse response = onboardingBillingService.createCheckout(sessionId, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/finalize")
    public ResponseEntity<OnboardingSessionResponse> finalizeProvisioning(
            @PathVariable UUID sessionId
    ) {
        OnboardingSessionResponse response = onboardingBillingService.finalizeProvisioning(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<OnboardingSessionResponse> getSession(
            @PathVariable UUID sessionId
    ) {
        OnboardingSessionResponse response = onboardingBillingService.getSession(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/stripe")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> stripeHealth() {
        return ResponseEntity.ok(stripeReadinessService.onboardingReadiness());
    }

    @PostMapping("/sessions/{sessionId}/auto-login")
    public ResponseEntity<AuthenticationResponse> autoLogin(
            @PathVariable UUID sessionId,
            HttpServletResponse response
    ) {
        AuthenticationResponse authResponse = onboardingBillingService.autoLogin(sessionId);
        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenCookieService.addRefreshTokenCookie(response, refreshToken);
            authResponse.setRefreshToken(null);
        }
        return ResponseEntity.ok(authResponse);
    }
}
