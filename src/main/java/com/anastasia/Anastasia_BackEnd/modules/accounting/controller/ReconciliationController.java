package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.BankStatementLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReconciliationResult;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// DTO for the reconciliation request body
@Data
class ReconciliationRequest {
    @Valid
    private List<BankStatementLine> statementLines;
    private BigDecimal closingBalance;
    private UUID tenantId;
    private Long accountId;
}

@RestController
@RequestMapping("/api/v1/accounting/reconciliation")
@RequiredArgsConstructor
@PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'RECONCILE_ACCOUNTS')")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/reconcile")
    public ResponseEntity<ReconciliationResult> reconcileStatement(
            @Valid @RequestBody ReconciliationRequest request) {

        ReconciliationResult result = reconciliationService.reconcileStatement(
                request.getTenantId(),
                request.getAccountId(),
                request.getStatementLines(),
                request.getClosingBalance()
        );

        return ResponseEntity.ok(result);
    }
}
