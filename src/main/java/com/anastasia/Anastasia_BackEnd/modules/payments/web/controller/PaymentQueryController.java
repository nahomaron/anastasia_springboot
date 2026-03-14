package com.anastasia.Anastasia_BackEnd.modules.payments.web.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.query.PaymentQueryService;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.PaymentView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final PaymentQueryService queryService;
    private final LocalizedMessageService messageService;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT', 'VIEW_DONATION_REPORTS')")
    @GetMapping
    public ResponseEntity<Page<PaymentView>> findAll(Pageable pageable) {
        UUID tenantUuid = requireTenantId();
        return ResponseEntity.ok(queryService.findAll(tenantUuid, pageable));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT', 'VIEW_DONATION_REPORTS')")
    @GetMapping("/{id}")
    public ResponseEntity<PaymentView> findById(@PathVariable UUID id) {
        UUID tenantUuid = requireTenantId();
        return ResponseEntity.ok(queryService.findById(tenantUuid, id));
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

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FINANCE_REPORT', 'VIEW_DONATION_REPORTS')")
    @GetMapping("/summary/funds")
    public ResponseEntity<List<Map<String, Object>>> totalPerFund() {
        UUID tenantUuid = requireTenantId();
        List<Map<String, Object>> totals = queryService.totalCapturedByFund(tenantUuid);
        return ResponseEntity.ok(totals);
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, messageService.get(
                    "tenant.context.missing",
                    "Tenant context is missing"
            ));
        }
        return tenantId;
    }

}
