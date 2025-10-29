package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordExpenseRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordIncomeRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransactionDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransferFundsRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounting/transactions")
@RequiredArgsConstructor
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
