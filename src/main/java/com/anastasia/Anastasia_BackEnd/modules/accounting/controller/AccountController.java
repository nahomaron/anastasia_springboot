package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.AccountDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateAccountRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.AccountService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ChartOfAccountsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final ChartOfAccountsService chartOfAccountsService;

    @PostMapping("/init-coa")
    public ResponseEntity<Void> createInitialChartOfAccounts(@RequestParam String tenantId) {
        chartOfAccountsService.createInitialChartOfAccounts(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDto createdAccount = accountService.createAccount(request);
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(
            @RequestParam String tenantId,
            @RequestParam(required = false) AccountType type) {

        List<AccountDto> accounts;
        if (type != null) {
            accounts = accountService.getAccountsByTenantIdAndType(tenantId, type);
        } else {
            accounts = accountService.getAccountsByTenantId(tenantId);
        }
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id, @RequestParam String tenantId) {
        return ResponseEntity.ok(accountService.getAccountById(id, tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable Long id,
            @RequestParam String tenantId,
            @Valid @RequestBody CreateAccountRequest request) {

        return ResponseEntity.ok(accountService.updateAccount(id, tenantId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id, @RequestParam String tenantId) {
        accountService.deleteAccount(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
