package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.modules.payments.application.query.PaymentQueryService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.query.SubscriptionQueryService;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentView;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.SubscriptionView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class SubscriptionQueryController {
    private final SubscriptionQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<SubscriptionView>> findAll(
            @RequestHeader("X-Tenant-Id") String tenantId,
            Pageable pageable) {
        return ResponseEntity.ok(queryService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionView> findById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(queryService.findById(tenantId, id));
    }

//    @GetMapping("/summary")
//    public ResponseEntity<Map<String, Object>> summary(@RequestHeader("X-Tenant-Id") String tenantId) {
//        long totalPayments = queryService.countAll(tenantId);
//        long totalCaptured = queryService.countByStatus(tenantId, "CAPTURED");
//        long activeSubs = queryService.countActiveSubscriptions(tenantId);
//        return ResponseEntity.ok(Map.of(
//                "totalPayments", totalPayments,
//                "totalCaptured", totalCaptured,
//                "activeSubscriptions", activeSubs
//        ));
//    }

}
