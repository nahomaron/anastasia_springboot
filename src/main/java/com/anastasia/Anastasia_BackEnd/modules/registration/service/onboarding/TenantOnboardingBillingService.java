package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.onboarding.OnboardingSessionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingSessionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantOnboardingSessionRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantOnboardingBillingService {

    private static final Set<SubscriptionPlan> LAUNCH_ONBOARDING_PLANS = Set.of(
            SubscriptionPlan.FREE,
            SubscriptionPlan.BASIC
    );

    private static final int DEFAULT_EXPIRY_HOURS = 24;

    private final TenantOnboardingSessionRepository onboardingSessionRepository;
    private final TenantPlanBillingCatalog billingCatalog;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final StripeClient stripeClient;
    private final TenantOnboardingProvisioningService onboardingProvisioningService;
    private final OnboardingEmailVerificationService onboardingEmailVerificationService;
    private final AuthService authService;
    private final LocalizedMessageService messageService;

    @Transactional
    public OnboardingSessionResponse createSession(TenantDTO tenantDTO, String idempotencyKey) {
        String normalizedIdempotency = normalizeIdempotency(idempotencyKey);
        return onboardingSessionRepository.findByIdempotencyKey(normalizedIdempotency)
                .map(this::toResponse)
                .orElseGet(() -> {
                    TenantOnboardingSessionEntity session = buildSession(tenantDTO, normalizedIdempotency);
                    return toResponse(onboardingSessionRepository.save(session));
                });
    }

    @Transactional
    public OnboardingSessionResponse createCheckout(UUID sessionId, String idempotencyKey) {
        TenantOnboardingSessionEntity session = onboardingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "onboarding.session.notFound",
                        "Onboarding session not found"
                )));

        ensureEmailVerifiedForOnboarding(session);

        if (!session.isPaymentRequired()) {
            if (session.getStatus() == OnboardingSessionStatus.DRAFT) {
                session.setStatus(OnboardingSessionStatus.CHECKOUT_SKIPPED);
                onboardingSessionRepository.save(session);
                onboardingProvisioningService.finalizeProvisioningIfReady(session.getId());
                session = onboardingSessionRepository.findById(session.getId()).orElse(session);
            }
            return toResponse(session);
        }

        if (session.getCheckoutUrl() != null && !session.getCheckoutUrl().isBlank()) {
            return toResponse(session);
        }

        if (session.getStatus() != OnboardingSessionStatus.DRAFT
                && session.getStatus() != OnboardingSessionStatus.CHECKOUT_CREATED
                && session.getStatus() != OnboardingSessionStatus.PAYMENT_PENDING) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.checkout.status.invalid",
                    "Session status does not allow checkout creation: {0}",
                    session.getStatus()
            ));
        }

        TenantPlanBillingCatalog.PlanPrice planPrice = billingCatalog.resolve(session.getSelectedPlan());
        Session stripeSession;
        try {
            stripeSession = stripeClient.createOnboardingSubscriptionCheckoutSession(
                    session.getId().toString(),
                    session.getOwnerEmail(),
                    planPrice.getPriceId(),
                    "TENANT_ONBOARDING_" + session.getSelectedPlan().name(),
                    normalizeIdempotency(idempotencyKey)
            );
        } catch (StripeException e) {
            throw mapCheckoutStripeException(e);
        }

        session.setCurrency(billingCatalog.getCurrency());
        session.setExpectedAmountMinor(planPrice.getAmountMinor());
        session.setCheckoutUrl(stripeSession.getUrl());
        session.setProviderCheckoutSessionId(stripeSession.getId());
        session.setStatus(OnboardingSessionStatus.CHECKOUT_CREATED);
        session.setCheckoutCreatedAt(Instant.now());

        return toResponse(onboardingSessionRepository.save(session));
    }

    @Transactional
    public OnboardingSessionResponse finalizeProvisioning(UUID sessionId) {
        onboardingProvisioningService.finalizeProvisioningIfReady(sessionId);
        TenantOnboardingSessionEntity session = onboardingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "onboarding.session.notFound",
                        "Onboarding session not found"
                )));
        return toResponse(session);
    }

    @Transactional
    public OnboardingSessionResponse getSession(UUID sessionId) {
        TenantOnboardingSessionEntity session = onboardingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "onboarding.session.notFound",
                        "Onboarding session not found"
                )));
        refreshFromStripeIfNeeded(session);
        return toResponse(session);
    }

    @Transactional
    public AuthenticationResponse autoLogin(UUID sessionId) {
        TenantOnboardingSessionEntity session = onboardingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "onboarding.session.notFound",
                        "Onboarding session not found"
                )));

        refreshFromStripeIfNeeded(session);
        if (session.getStatus() != OnboardingSessionStatus.PROVISIONED) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.session.notProvisioned",
                    "Session is not provisioned yet."
            ));
        }
        if (session.getProvisionedOwnerUserId() == null) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.session.ownerMissing",
                    "Provisioned owner user was not found for this session."
            ));
        }

        return authService.issueSessionForUser(session.getProvisionedOwnerUserId());
    }

    private TenantOnboardingSessionEntity buildSession(TenantDTO tenantDTO, String normalizedIdempotency) {
        if (!tenantDTO.isPasswordMatch()) {
            throw new IllegalArgumentException(messageService.get(
                    "onboarding.password.mismatch",
                    "Password and confirm password do not match"
            ));
        }
        if (!Boolean.TRUE.equals(tenantDTO.getTermsAccepted())) {
            throw new IllegalArgumentException(messageService.get(
                    "onboarding.terms.required",
                    "Terms and Conditions must be accepted before payment."
            ));
        }
        if (tenantDTO.getTermsVersion() == null || tenantDTO.getTermsVersion().isBlank()) {
            throw new IllegalArgumentException(messageService.get(
                    "onboarding.terms.version.required",
                    "Terms version is required."
            ));
        }
        if (!LAUNCH_ONBOARDING_PLANS.contains(tenantDTO.getSubscriptionPlan())) {
            throw new IllegalArgumentException(messageService.get(
                    "onboarding.plan.unsupported",
                    "Selected plan is not available for self-service onboarding yet."
            ));
        }

        boolean paymentRequired = tenantDTO.getSubscriptionPlan() != SubscriptionPlan.FREE;
        Long expectedAmountMinor = null;
        String currency = null;
        if (paymentRequired) {
            TenantPlanBillingCatalog.PlanPrice planPrice = billingCatalog.resolve(tenantDTO.getSubscriptionPlan());
            expectedAmountMinor = planPrice.getAmountMinor();
            currency = billingCatalog.getCurrency();
        }

        return TenantOnboardingSessionEntity.builder()
                .idempotencyKey(normalizedIdempotency)
                .status(OnboardingSessionStatus.DRAFT)
                .tenantType(tenantDTO.getTenantType())
                .selectedPlan(tenantDTO.getSubscriptionPlan())
                .ownerName(tenantDTO.getOwnerName())
                .ownerEmail(tenantDTO.getOwnerEmail())
                .ownerPhone(tenantDTO.getPhoneNumber())
                .termsAccepted(true)
                .termsAcceptedAt(Instant.now())
                .termsVersion(tenantDTO.getTermsVersion().trim())
                .draftPayloadJson(toSanitizedDraftPayload(tenantDTO))
                .draftPasswordHash(passwordEncoder.encode(tenantDTO.getPassword()))
                .paymentRequired(paymentRequired)
                .currency(currency)
                .expectedAmountMinor(expectedAmountMinor)
                .expiresAt(Instant.now().plusSeconds(DEFAULT_EXPIRY_HOURS * 60L * 60L))
                .build();
    }

    private String toSanitizedDraftPayload(TenantDTO tenantDTO) {
        Map<String, Object> payload = objectMapper.convertValue(tenantDTO, Map.class);
        payload.remove("password");
        payload.remove("confirmPassword");
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.session.draftPayload.serializeFailed",
                    "Failed to serialize onboarding draft payload"
            ), e);
        }
    }

    private String normalizeIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(messageService.get(
                    "onboarding.idempotency.required",
                    "Idempotency-Key header is required"
            ));
        }
        return idempotencyKey.trim();
    }

    private void ensureEmailVerifiedForOnboarding(TenantOnboardingSessionEntity session) {
        if (!onboardingEmailVerificationService.isVerified(session.getOwnerEmail())) {
            throw new IllegalStateException(messageService.get(
                    "onboarding.emailVerification.required",
                    "Email must be verified before continuing to checkout."
            ));
        }
    }

    private void refreshFromStripeIfNeeded(TenantOnboardingSessionEntity session) {
        if (!session.isPaymentRequired()) {
            return;
        }
        if (session.getStatus() == OnboardingSessionStatus.PROVISIONED
                || session.getStatus() == OnboardingSessionStatus.PROVISIONING_FAILED
                || session.getStatus() == OnboardingSessionStatus.CANCELED
                || session.getStatus() == OnboardingSessionStatus.EXPIRED) {
            return;
        }
        if (session.getProviderCheckoutSessionId() == null || session.getProviderCheckoutSessionId().isBlank()) {
            return;
        }

        try {
            Session checkoutSession = stripeClient.retrieveCheckoutSession(session.getProviderCheckoutSessionId());

            if (checkoutSession.getCustomer() != null && !checkoutSession.getCustomer().isBlank()) {
                session.setProviderCustomerId(checkoutSession.getCustomer());
            }

            String subscriptionId = checkoutSession.getSubscription();
            if (subscriptionId == null || subscriptionId.isBlank()) {
                String checkoutStatus = checkoutSession.getStatus() == null ? "" : checkoutSession.getStatus().toLowerCase();
                if ("expired".equals(checkoutStatus)) {
                    session.setStatus(OnboardingSessionStatus.EXPIRED);
                    session.setFailureReason(messageService.get(
                            "onboarding.stripe.checkout.expired",
                            "Stripe checkout session expired."
                    ));
                    onboardingSessionRepository.save(session);
                }
                return;
            }

            session.setProviderSubscriptionId(subscriptionId);
            Subscription subscription = stripeClient.retrieveSubscription(subscriptionId);
            if (subscription.getCustomer() != null && !subscription.getCustomer().isBlank()) {
                session.setProviderCustomerId(subscription.getCustomer());
            }

            String stripeStatus = subscription.getStatus() == null ? "" : subscription.getStatus().toLowerCase();
            switch (stripeStatus) {
                case "active", "trialing" -> {
                    if (session.getPaymentConfirmedAt() == null) {
                        session.setPaymentConfirmedAt(Instant.now());
                    }
                    session.setStatus(OnboardingSessionStatus.PAYMENT_CONFIRMED);
                    session.setFailureReason(null);
                    onboardingSessionRepository.save(session);
                    try {
                        onboardingProvisioningService.finalizeProvisioningIfReady(session.getId());
                    } catch (RuntimeException ex) {
                        log.warn("Provisioning reconciliation failed for session {}: {}", session.getId(), ex.getMessage());
                    }
                }
                case "canceled", "unpaid", "incomplete_expired" -> {
                    session.setStatus(OnboardingSessionStatus.CANCELED);
                    session.setFailureReason(messageService.get(
                            "onboarding.stripe.subscription.status",
                            "Stripe subscription status is {0}",
                            stripeStatus
                    ));
                    onboardingSessionRepository.save(session);
                }
                default -> {
                    session.setStatus(OnboardingSessionStatus.PAYMENT_PENDING);
                    onboardingSessionRepository.save(session);
                }
            }
        } catch (StripeException ex) {
            log.warn("Stripe reconciliation failed for onboarding session {}: {}", session.getId(), ex.getMessage());
        }
    }

    private IllegalStateException mapCheckoutStripeException(StripeException ex) {
        if (ex instanceof AuthenticationException) {
            return new IllegalStateException(
                    messageService.get(
                            "onboarding.stripe.configuration.invalid",
                            "Stripe is not configured on the server. Missing or invalid Stripe API key."
                    ),
                    ex
            );
        }

        if (ex instanceof InvalidRequestException) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.contains("no such price")) {
                return new IllegalStateException(
                        messageService.get(
                                "onboarding.stripe.pricing.invalid",
                                "Stripe pricing is not configured correctly for the selected plan."
                        ),
                        ex
                );
            }
        }

        return new IllegalStateException(messageService.get(
                "onboarding.stripe.checkout.createFailed",
                "Unable to create Stripe onboarding checkout session"
        ), ex);
    }

    private OnboardingSessionResponse toResponse(TenantOnboardingSessionEntity session) {
        return OnboardingSessionResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .tenantType(session.getTenantType())
                .selectedPlan(session.getSelectedPlan())
                .ownerName(session.getOwnerName())
                .ownerEmail(session.getOwnerEmail())
                .ownerPhone(session.getOwnerPhone())
                .paymentRequired(session.isPaymentRequired())
                .currency(session.getCurrency())
                .expectedAmountMinor(session.getExpectedAmountMinor())
                .checkoutUrl(session.getCheckoutUrl())
                .checkoutSessionId(session.getProviderCheckoutSessionId())
                .provisionedTenantId(session.getProvisionedTenantId())
                .provisionedOwnerUserId(session.getProvisionedOwnerUserId())
                .failureReason(session.getFailureReason())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }
}
