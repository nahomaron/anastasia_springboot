package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreatePaymentIntentUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateIntentRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentIntentUseCase createIntent;

    @PostMapping("/intents")
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateIntentRequest req) {

        var pi = createIntent.execute(
                tenantId, req.getPurpose(), req.getAmount(), req.getCurrency(),
                req.getMemberId(), req.getFundId(), idempotencyKey);

        var resp = new PaymentResponse();
        resp.setPaymentId(pi.getId());
        resp.setStatus(pi.getStatus().name());
        resp.setCheckoutUrl(pi.getCheckoutUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}
