package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.AccountDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateAccountRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.security.AccountingTenantResolver;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.AccountService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ChartOfAccountsService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/accounts")
@RequiredArgsConstructor
@RequiresTenantFeature(TenantFeature.FINANCE_ACCOUNTING)
@PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_ACCOUNTS', 'MANAGE_ACCOUNTS')")
public class AccountController {

    private final AccountService accountService;
    private final ChartOfAccountsService chartOfAccountsService;
    private final AccountingTenantResolver tenantResolver;

    @PostMapping("/init-coa")
    public ResponseEntity<Void> createInitialChartOfAccounts(@RequestParam(required = false) UUID tenantId) {
        chartOfAccountsService.createInitialChartOfAccounts(tenantResolver.resolveTenant(tenantId));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        request.setTenantId(tenantResolver.resolveTenant(request.getTenantId()));
        AccountDto createdAccount = accountService.createAccount(request);
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) AccountType type) {

        UUID effectiveTenantId = tenantResolver.resolveTenant(tenantId);
        List<AccountDto> accounts;
        if (type != null) {
            accounts = accountService.getAccountsByTenantIdAndType(effectiveTenantId, type);
        } else {
            accounts = accountService.getAccountsByTenantId(effectiveTenantId);
        }
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id, @RequestParam(required = false) UUID tenantId) {
        return ResponseEntity.ok(accountService.getAccountById(id, tenantResolver.resolveTenant(tenantId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable Long id,
            @RequestParam(required = false) UUID tenantId,
            @Valid @RequestBody CreateAccountRequest request) {

        UUID effectiveTenantId = tenantResolver.resolveTenant(tenantId);
        request.setTenantId(tenantResolver.resolveTenant(request.getTenantId()));
        return ResponseEntity.ok(accountService.updateAccount(id, effectiveTenantId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id, @RequestParam(required = false) UUID tenantId) {
        accountService.deleteAccount(id, tenantResolver.resolveTenant(tenantId));
        return ResponseEntity.noContent().build();
    }
}
