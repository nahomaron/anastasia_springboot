package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;


import com.anastasia.Anastasia_BackEnd.modules.payments.application.query.PaymentQueryService;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final PaymentQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<PaymentView>> findAll(
            @RequestHeader("X-Tenant-Id") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(queryService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentView> findById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(queryService.findById(tenantId, id));
    }

//    @GetMapping("/summary")
//    public ResponseEntity<Map<String, Object>> summary(@RequestHeader("X-Tenant-Id") String tenantId) {
//        long totalPayments = paymentRepo.countByTenantId(tenantId);
//        long totalCaptured = paymentRepo.countByTenantIdAndStatus(tenantId, PaymentStatus.CAPTURED);
//        long activeSubs = subRepo.countByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE);
//        return ResponseEntity.ok(Map.of(
//                "totalPayments", totalPayments,
//                "totalCaptured", totalCaptured,
//                "activeSubscriptions", activeSubs
//        ));
//    }

    @GetMapping("/summary/funds")
    public ResponseEntity<List<Map<String, Object>>> totalPerFund(
            @RequestHeader("X-Tenant-Id") String tenantId) {

        List<Map<String, Object>> totals = queryService.totalCapturedByFund(tenantId);
        return ResponseEntity.ok(totals);
    }


}
