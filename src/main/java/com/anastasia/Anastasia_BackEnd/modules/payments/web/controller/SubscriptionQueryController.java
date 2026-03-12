package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.query.SubscriptionQueryService;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.SubscriptionView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/subscriptions")
@RequiredArgsConstructor
public class SubscriptionQueryController {
    private final SubscriptionQueryService queryService;
    private final LocalizedMessageService messageService;

    @GetMapping
    public ResponseEntity<Page<SubscriptionView>> findAll(
            @RequestHeader("X-Tenant-Id") String tenantId,
            Pageable pageable) {
        UUID tenantUuid = parseTenantId(tenantId);
        return ResponseEntity.ok(queryService.findAll(tenantUuid, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionView> findById(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID id) {
        UUID tenantUuid = parseTenantId(tenantId);
        return ResponseEntity.ok(queryService.findById(tenantUuid, id));
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
}
