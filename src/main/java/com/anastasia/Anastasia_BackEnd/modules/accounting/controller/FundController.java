package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.FundDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.UpdateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.FundService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/funds")
@RequiredArgsConstructor
@RequiresTenantFeature(TenantFeature.FINANCE_ACCOUNTING)
@PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'VIEW_FUNDS', 'MANAGE_FUNDS')")
@Tag(name = "Accounting Funds")
public class FundController {

    private final FundService fundService;

    @PostMapping
    public ResponseEntity<FundDto> createFund(@Valid @RequestBody CreateFundRequest request) {
        FundDto createdFund = fundService.createFund(request);
        return new ResponseEntity<>(createdFund, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FundDto>> getFunds(@RequestParam UUID tenantId) {
        return ResponseEntity.ok(fundService.getFundsByTenantId(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FundDto> getFundById(@PathVariable Long id, @RequestParam UUID tenantId) {
        return ResponseEntity.ok(fundService.getFundById(id, tenantId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a tenant fund definition")
    public ResponseEntity<FundDto> updateFund(@PathVariable Long id, @Valid @RequestBody UpdateFundRequest request) {
        return ResponseEntity.ok(fundService.updateFund(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tenant fund when it has no balance or ledger references")
    public ResponseEntity<Void> deleteFund(@PathVariable Long id, @RequestParam UUID tenantId) {
        fundService.deleteFund(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
