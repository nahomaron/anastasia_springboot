package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.GenerateReportRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReportResponseDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ReportService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounting/reports")
@RequiredArgsConstructor
@RequiresTenantFeature(TenantFeature.REPORTING)
public class ReportController {

    private final ReportService reportService;

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'GENERATE_FINANCE_REPORT')")
    @PostMapping("/generate")
    public ResponseEntity<ReportResponseDto> generateReport(@Valid @RequestBody GenerateReportRequest request) {
        ReportResponseDto report = reportService.generateReport(request);
        return ResponseEntity.ok(report);
    }
}
