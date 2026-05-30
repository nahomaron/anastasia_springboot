package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordExpenseRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordIncomeRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransactionDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransferFundsRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.security.AccountingTenantResolver;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/transactions")
@RequiredArgsConstructor
@RequiresTenantFeature(TenantFeature.FINANCE_ACCOUNTING)
@PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'RECORD_TRANSACTIONS')")
@Tag(name = "Accounting Transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountingTenantResolver tenantResolver;

    @PostMapping("/income")
    public ResponseEntity<TransactionDto> recordIncome(@Valid @RequestBody RecordIncomeRequest request) {
        request.setTenantId(tenantResolver.resolveTenant(request.getTenantId()));
        TransactionDto transaction = transactionService.recordIncome(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @PostMapping("/expense")
    public ResponseEntity<TransactionDto> recordExpense(@Valid @RequestBody RecordExpenseRequest request) {
        request.setTenantId(tenantResolver.resolveTenant(request.getTenantId()));
        TransactionDto transaction = transactionService.recordExpense(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transferFunds(@Valid @RequestBody TransferFundsRequest request) {
        request.setTenantId(tenantResolver.resolveTenant(request.getTenantId()));
        TransactionDto transaction = transactionService.transferFunds(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Retrieve a single transaction by tenant-scoped id")
    public ResponseEntity<TransactionDto> getTransactionById(
            @PathVariable Long transactionId,
            @RequestParam(required = false) UUID tenantId
    ) {
        return ResponseEntity.ok(transactionService.getTransactionById(transactionId, tenantResolver.resolveTenant(tenantId)));
    }

    @GetMapping
    @Operation(summary = "List tenant transactions with optional date and account filters")
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long accountId
    ) {
        return ResponseEntity.ok(transactionService.getTransactions(
                tenantResolver.resolveTenant(tenantId),
                startDate,
                endDate,
                accountId
        ));
    }
}
