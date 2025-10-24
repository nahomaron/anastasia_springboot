package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreatePaymentIntentUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreateSubscriptionUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateIntentRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateSubscriptionRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentResponse;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final CreatePaymentIntentUseCase createIntent;
    private final CreateSubscriptionUseCase createSubscription;

    @PostMapping("/intents")
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader("X-Tenant-Id") @NotBlank String tenantId,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateIntentRequest req) {

        var pi = createIntent.execute(
                tenantId, req.getPurpose(), req.getAmount(), req.getCurrency(),
                req.getMemberId(), req.getFundId(), idempotencyKey);

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

        var subscription = createSubscription.execute(
                tenantId,
                req.getPurpose(),
                req.getAmount(),
                req.getCurrency(),
                req.getMemberId(),
                req.getFundId(),
                idempotencyKey);

        var resp = new SubscriptionResponse();
        resp.setSubscriptionId(subscription.getId());
        resp.setStatus(subscription.getStatus().name());
        resp.setCheckoutUrl(subscription.getCheckoutUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}
