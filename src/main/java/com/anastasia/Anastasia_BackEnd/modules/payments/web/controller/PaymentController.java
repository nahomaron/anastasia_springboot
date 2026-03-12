package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreateSubscriptionUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.saga.PaymentCheckoutSaga;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateIntentRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateSubscriptionRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentResponse;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.SubscriptionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementResolverService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentCheckoutSaga checkoutSaga;
    private final CreateSubscriptionUseCase createSubscription;
    private final EntitlementResolverService entitlementResolverService;
    private final LocalizedMessageService messageService;

    @PostMapping("/intents")
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateIntentRequest req) {

        UUID tenantUuid = parseTenantId(tenantId);
        ensureStewardshipEnabled(tenantUuid);
        var pi = checkoutSaga.startCheckout(
                tenantUuid,
                req.getPurpose(),
                req.getAmount(),
                req.getCurrency(),
                req.getMemberId(),
                req.getUserId(),
                req.getUserEmail(),
                req.getFundId(),
                idempotencyKey);

        var resp = new PaymentResponse();
        resp.setPaymentId(pi.getId());
        resp.setStatus(pi.getStatus().name());
        resp.setCheckoutUrl(pi.getCheckoutUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateSubscriptionRequest req) {

        UUID tenantUuid = parseTenantId(tenantId);
        ensureStewardshipEnabled(tenantUuid);
        var subscription = createSubscription.execute(
                tenantUuid,
                req.getPurpose(),
                req.getAmount(),
                req.getCurrency(),
                req.getMemberId(),
                req.getUserId(),
                req.getUserEmail(),
                req.getFundId(),
                idempotencyKey);

        var resp = new SubscriptionResponse();
        resp.setSubscriptionId(subscription.getId());
        resp.setStatus(subscription.getStatus().name());
        resp.setCheckoutUrl(subscription.getCheckoutUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    private UUID parseTenantId(String headerValue) {
        try {
            return UUID.fromString(headerValue);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, messageService.get(
                    "payments.tenantId.invalid",
                    "Invalid tenant id"
            ));
        }
    }

    private void ensureStewardshipEnabled(UUID tenantId) {
        if (!entitlementResolverService.hasFeature(tenantId, TenantFeature.STEWARDSHIP_GIVING)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, messageService.get(
                    "payments.stewardship.disabled",
                    "Stewardship/giving is not enabled for this tenant plan"
            ));
        }
    }
}
