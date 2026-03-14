package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordExpenseRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordIncomeRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransactionDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransferFundsRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounting/transactions")
@RequiredArgsConstructor
@RequiresTenantFeature(TenantFeature.FINANCE_ACCOUNTING)
@PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'RECORD_TRANSACTIONS')")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/income")
    public ResponseEntity<TransactionDto> recordIncome(@Valid @RequestBody RecordIncomeRequest request) {
        TransactionDto transaction = transactionService.recordIncome(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @PostMapping("/expense")
    public ResponseEntity<TransactionDto> recordExpense(@Valid @RequestBody RecordExpenseRequest request) {
        TransactionDto transaction = transactionService.recordExpense(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transferFunds(@Valid @RequestBody TransferFundsRequest request) {
        TransactionDto transaction = transactionService.transferFunds(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    // TODO: Add GET endpoints to retrieve transactions (by ID, by date range, by account)
}
