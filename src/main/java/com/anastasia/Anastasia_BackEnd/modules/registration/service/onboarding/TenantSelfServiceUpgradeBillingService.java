package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.stripe.StripeClient;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SubscriptionUpgradeCheckoutResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionProviderLinkEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionUpgradeRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionUpgradeStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionProviderLinkRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionUpgradeRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantSelfServiceUpgradeBillingService {

    private static final Set<TenantSubscriptionUpgradeStatus> ACTIVE_STATUSES = EnumSet.of(
            TenantSubscriptionUpgradeStatus.PENDING_CHECKOUT,
            TenantSubscriptionUpgradeStatus.CHECKOUT_COMPLETED
    );

    private final SubscriptionService subscriptionService;
    private final TenantPlanBillingCatalog billingCatalog;
    private final TenantSubscriptionProviderLinkRepository tenantSubscriptionProviderLinkRepository;
    private final TenantSubscriptionUpgradeRequestRepository tenantSubscriptionUpgradeRequestRepository;
    private final StripeClient stripeClient;
    private final LocalizedMessageService messageService;

    @Transactional
    public SubscriptionUpgradeCheckoutResponse createUpgradeCheckout(UUID tenantId,
                                                                    SubscriptionPlan targetPlan,
                                                                    String idempotencyKey,
                                                                    UUID actorUserId) {
        String normalizedIdempotency = normalizeIdempotency(idempotencyKey);
        TenantSubscriptionUpgradeRequestEntity existing = tenantSubscriptionUpgradeRequestRepository
                .findByIdempotencyKey(normalizedIdempotency)
                .orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        TenantSubscriptionEntity subscription = subscriptionService.syncSubscriptionState(tenantId, actorUserId);
        SubscriptionPlan currentPlan = subscription.getPlan() != null ? subscription.getPlan() : SubscriptionPlan.FREE;

        if (targetPlan == null) {
            throw new IllegalArgumentException(messageService.get(
                    "subscription.planChange.target.required",
                    "targetPlan is required"
            ));
        }
        if (currentPlan.rank() >= targetPlan.rank()) {
            throw new IllegalArgumentException(messageService.get(
                    "subscription.planChange.upgrade.only",
                    "Checkout-backed plan changes are only available for upgrades."
            ));
        }
        if (currentPlan != SubscriptionPlan.FREE || targetPlan != SubscriptionPlan.BASIC) {
            throw new IllegalArgumentException(messageService.get(
                    "subscription.planChange.selfService.unsupported",
                    "Self-service checkout is currently available only for upgrading from FREE to BASIC."
            ));
        }

        tenantSubscriptionUpgradeRequestRepository
                .findFirstByTenantSubscription_IdAndStatusInOrderByCreatedAtDesc(subscription.getId(), ACTIVE_STATUSES)
                .ifPresent(activeRequest -> {
                    if (activeRequest.getTargetPlan() == targetPlan) {
                        throw new IllegalStateException(messageService.get(
                                "subscription.planChange.checkout.alreadyPending",
                                "A checkout session is already pending for this upgrade."
                        ));
                    }
                    throw new IllegalStateException(messageService.get(
                            "subscription.planChange.checkout.conflict",
                            "Another self-service upgrade request is already in progress."
                    ));
                });

        TenantPlanBillingCatalog.PlanPrice planPrice = billingCatalog.resolve(targetPlan);
        TenantSubscriptionProviderLinkEntity providerLink = tenantSubscriptionProviderLinkRepository
                .findByTenantSubscription_IdAndProvider(subscription.getId(), BillingProvider.STRIPE)
                .orElse(null);

        TenantSubscriptionUpgradeRequestEntity request = TenantSubscriptionUpgradeRequestEntity.builder()
                .tenantSubscription(subscription)
                .currentPlan(currentPlan)
                .targetPlan(targetPlan)
                .provider(BillingProvider.STRIPE)
                .status(TenantSubscriptionUpgradeStatus.PENDING_CHECKOUT)
                .idempotencyKey(normalizedIdempotency)
                .currency(billingCatalog.getCurrency())
                .expectedAmountMinor(planPrice.getAmountMinor())
                .providerPriceReference(planPrice.getPriceId())
                .expiresAt(Instant.now().plusSeconds(24L * 60L * 60L))
                .createdByUserId(actorUserId)
                .updatedByUserId(actorUserId)
                .build();
        tenantSubscriptionUpgradeRequestRepository.save(request);

        Session checkoutSession;
        try {
            checkoutSession = stripeClient.createTenantUpgradeSubscriptionCheckoutSession(
                    request.getId().toString(),
                    providerLink != null ? providerLink.getProviderCustomerId() : null,
                    planPrice.getPriceId(),
                    "TENANT_SELF_SERVICE_UPGRADE_" + targetPlan.name(),
                    normalizedIdempotency
            );
        } catch (StripeException ex) {
            request.setStatus(TenantSubscriptionUpgradeStatus.FAILED);
            request.setFailureReason(trimFailureReason(ex.getMessage()));
            tenantSubscriptionUpgradeRequestRepository.save(request);
            throw mapCheckoutStripeException(ex);
        }

        request.setCheckoutUrl(checkoutSession.getUrl());
        request.setProviderCheckoutSessionId(checkoutSession.getId());
        request.setUpdatedByUserId(actorUserId);
        return toResponse(tenantSubscriptionUpgradeRequestRepository.save(request));
    }

    @Transactional
    public boolean markCheckoutCompleted(String checkoutSessionId,
                                         String providerCustomerId,
                                         String providerSubscriptionId) {
        if (checkoutSessionId == null || checkoutSessionId.isBlank()) {
            return false;
        }
        TenantSubscriptionUpgradeRequestEntity request = tenantSubscriptionUpgradeRequestRepository
                .findByProviderCheckoutSessionId(checkoutSessionId)
                .orElse(null);
        if (request == null) {
            return false;
        }
        if (request.getStatus() == TenantSubscriptionUpgradeStatus.PAYMENT_CONFIRMED) {
            return true;
        }

        request.setStatus(TenantSubscriptionUpgradeStatus.CHECKOUT_COMPLETED);
        request.setProviderCustomerId(normalizeBlank(providerCustomerId));
        request.setProviderSubscriptionId(normalizeBlank(providerSubscriptionId));
        request.setCheckoutCompletedAt(Instant.now());
        request.setFailureReason(null);
        tenantSubscriptionUpgradeRequestRepository.save(request);
        return true;
    }

    @Transactional
    public boolean markSubscriptionPending(String upgradeRequestId,
                                           String providerCustomerId,
                                           String providerSubscriptionId) {
        if (upgradeRequestId == null || upgradeRequestId.isBlank()) {
            return false;
        }
        Optional<TenantSubscriptionUpgradeRequestEntity> existing = tenantSubscriptionUpgradeRequestRepository
                .findById(UUID.fromString(upgradeRequestId));
        if (existing.isEmpty()) {
            return false;
        }
        TenantSubscriptionUpgradeRequestEntity request = existing.get();
        if (request.getStatus() == TenantSubscriptionUpgradeStatus.PAYMENT_CONFIRMED) {
            return true;
        }

        request.setStatus(TenantSubscriptionUpgradeStatus.CHECKOUT_COMPLETED);
        request.setProviderCustomerId(normalizeBlank(providerCustomerId));
        request.setProviderSubscriptionId(normalizeBlank(providerSubscriptionId));
        request.setFailureReason(null);
        if (request.getCheckoutCompletedAt() == null) {
            request.setCheckoutCompletedAt(Instant.now());
        }
        tenantSubscriptionUpgradeRequestRepository.save(request);
        return true;
    }

    @Transactional
    public boolean confirmPayment(String providerSubscriptionId,
                                  String providerCustomerId,
                                  Instant paymentAt,
                                  String providerEventId) {
        if (providerSubscriptionId == null || providerSubscriptionId.isBlank()) {
            return false;
        }
        TenantSubscriptionUpgradeRequestEntity request = tenantSubscriptionUpgradeRequestRepository
                .findByProviderAndProviderSubscriptionId(BillingProvider.STRIPE, providerSubscriptionId)
                .orElse(null);
        if (request == null) {
            return false;
        }
        if (request.getStatus() == TenantSubscriptionUpgradeStatus.PAYMENT_CONFIRMED) {
            return true;
        }

        Instant effectivePaymentAt = paymentAt != null ? paymentAt : Instant.now();
        request.setStatus(TenantSubscriptionUpgradeStatus.PAYMENT_CONFIRMED);
        request.setProviderCustomerId(normalizeBlank(providerCustomerId));
        request.setProviderSubscriptionId(providerSubscriptionId);
        request.setPaymentConfirmedAt(effectivePaymentAt);
        request.setFailureReason(null);
        tenantSubscriptionUpgradeRequestRepository.save(request);

        subscriptionService.activatePaidPlan(
                request.getTenantSubscription().getTenant().getId(),
                request.getTargetPlan(),
                effectivePaymentAt,
                BillingProvider.STRIPE,
                request.getProviderCustomerId(),
                providerSubscriptionId,
                request.getProviderPriceReference(),
                request.getUpdatedByUserId(),
                "Self-service checkout upgrade confirmed",
                providerEventId
        );
        return true;
    }

    private String normalizeIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(messageService.get(
                    "subscription.planChange.idempotency.required",
                    "Idempotency-Key header is required"
            ));
        }
        return idempotencyKey.trim();
    }

    private IllegalStateException mapCheckoutStripeException(StripeException ex) {
        if (ex instanceof AuthenticationException) {
            return new IllegalStateException(messageService.get(
                    "subscription.planChange.stripe.configuration.invalid",
                    "Stripe is not configured on the server. Missing or invalid Stripe API key."
            ), ex);
        }
        if (ex instanceof InvalidRequestException) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
            if (message.contains("no such price")) {
                return new IllegalStateException(messageService.get(
                        "subscription.planChange.stripe.pricing.invalid",
                        "Stripe pricing is not configured correctly for the selected plan."
                ), ex);
            }
        }
        return new IllegalStateException(messageService.get(
                "subscription.planChange.stripe.checkout.createFailed",
                "Unable to create Stripe checkout session for the requested upgrade."
        ), ex);
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String trimFailureReason(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown upgrade checkout error";
        }
        return message.length() > 512 ? message.substring(0, 512) : message;
    }

    private SubscriptionUpgradeCheckoutResponse toResponse(TenantSubscriptionUpgradeRequestEntity request) {
        return SubscriptionUpgradeCheckoutResponse.builder()
                .upgradeRequestId(request.getId())
                .tenantId(request.getTenantSubscription().getTenant().getId())
                .currentPlan(request.getCurrentPlan())
                .targetPlan(request.getTargetPlan())
                .status(request.getStatus())
                .currency(request.getCurrency())
                .expectedAmountMinor(request.getExpectedAmountMinor())
                .checkoutUrl(request.getCheckoutUrl())
                .checkoutSessionId(request.getProviderCheckoutSessionId())
                .expiresAt(request.getExpiresAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
