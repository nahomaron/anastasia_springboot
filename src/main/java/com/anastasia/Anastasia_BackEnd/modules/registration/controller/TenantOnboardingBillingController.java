package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.onboarding.OnboardingSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingBillingReadinessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingSessionAccessService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantOnboardingBillingService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tenant Onboarding Billing")
public class TenantOnboardingBillingController {

    private final TenantOnboardingBillingService onboardingBillingService;
    private final StripeReadinessService stripeReadinessService;
    private final OnboardingBillingReadinessService onboardingBillingReadinessService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final OnboardingSessionAccessService onboardingSessionAccessService;

    @PostMapping("/sessions")
    public ResponseEntity<OnboardingSessionResponse> createOnboardingSession(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody TenantDTO tenantDTO,
            @RequestHeader(value = OnboardingSessionAccessService.ONBOARDING_ACCESS_HEADER_NAME, required = false)
            String onboardingAccessToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        OnboardingSessionResponse sessionResponse = onboardingBillingService.createSession(
                tenantDTO,
                idempotencyKey,
                resolveOnboardingAccessToken(onboardingAccessToken, request)
        );
        if (sessionResponse.getOnboardingAccessToken() != null && !sessionResponse.getOnboardingAccessToken().isBlank()) {
            onboardingSessionAccessService.addAccessTokenCookie(
                    response,
                    sessionResponse.getOnboardingAccessToken(),
                    sessionResponse.getExpiresAt()
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionResponse);
    }

    @PostMapping("/sessions/{sessionId}/checkout")
    public ResponseEntity<OnboardingSessionResponse> createCheckout(
            @PathVariable UUID sessionId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader(value = OnboardingSessionAccessService.ONBOARDING_ACCESS_HEADER_NAME, required = false)
            String onboardingAccessToken,
            HttpServletRequest request
    ) {
        OnboardingSessionResponse response = onboardingBillingService.createCheckout(
                sessionId,
                idempotencyKey,
                resolveOnboardingAccessToken(onboardingAccessToken, request)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sessions/{sessionId}/finalize")
    public ResponseEntity<OnboardingSessionResponse> finalizeProvisioning(
            @PathVariable UUID sessionId,
            @RequestHeader(value = OnboardingSessionAccessService.ONBOARDING_ACCESS_HEADER_NAME, required = false)
            String onboardingAccessToken,
            HttpServletRequest request
    ) {
        OnboardingSessionResponse response = onboardingBillingService.finalizeProvisioning(
                sessionId,
                resolveOnboardingAccessToken(onboardingAccessToken, request)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<OnboardingSessionResponse> getSession(
            @PathVariable UUID sessionId,
            @RequestHeader(value = OnboardingSessionAccessService.ONBOARDING_ACCESS_HEADER_NAME, required = false)
            String onboardingAccessToken,
            HttpServletRequest request
    ) {
        OnboardingSessionResponse response = onboardingBillingService.getSession(
                sessionId,
                resolveOnboardingAccessToken(onboardingAccessToken, request)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health/stripe")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Inspect Stripe onboarding readiness for platform administrators")
    public ResponseEntity<Map<String, Object>> stripeHealth() {
        return ResponseEntity.ok(stripeReadinessService.onboardingReadiness());
    }

    @GetMapping("/health/runtime")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Inspect runtime readiness for staging onboarding flows")
    public ResponseEntity<Map<String, Object>> runtimeHealth() {
        return ResponseEntity.ok(onboardingBillingReadinessService.runtimeReadiness());
    }

    @PostMapping("/sessions/{sessionId}/auto-login")
    public ResponseEntity<AuthenticationResponse> autoLogin(
            @PathVariable UUID sessionId,
            @RequestHeader(value = OnboardingSessionAccessService.ONBOARDING_ACCESS_HEADER_NAME, required = false)
            String onboardingAccessToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthenticationResponse authResponse = onboardingBillingService.autoLogin(
                sessionId,
                resolveOnboardingAccessToken(onboardingAccessToken, request)
        );
        String refreshToken = authResponse.getRefreshToken();
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenCookieService.addRefreshTokenCookie(response, refreshToken);
            authResponse.setRefreshToken(null);
        }
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/eligibility")
    public ResponseEntity<Map<String, Object>> validateEligibility(
            @Valid @RequestBody OnboardingEligibilityRequest request
    ) {
        onboardingBillingService.validateOwnerIdentity(request.ownerEmail(), request.phoneNumber());
        return ResponseEntity.ok(Map.of(
                "eligible", true,
                "message", "Owner identity is eligible for onboarding."
        ));
    }

    private String resolveOnboardingAccessToken(String onboardingAccessToken, HttpServletRequest request) {
        if (onboardingAccessToken != null && !onboardingAccessToken.isBlank()) {
            return onboardingAccessToken;
        }
        return onboardingSessionAccessService.extractAccessToken(request).orElse(null);
    }

    public record OnboardingEligibilityRequest(
            @NotBlank String ownerEmail,
            @NotBlank String phoneNumber
    ) {}
}
