package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;


import com.anastasia.Anastasia_BackEnd.modules.accounting.security.AccountingTenantResolver;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ImportExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/io")
@RequiredArgsConstructor
public class ImportExportController {

    private final ImportExportService importExportService;
    private final AccountingTenantResolver tenantResolver;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'EXPORT_FINANCIAL_DATA')")
    @GetMapping("/export/quickbooks")
    public void exportToQuickBooks(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) {

        UUID effectiveTenantId = tenantResolver.resolveTenant(tenantId);
        try {
            response.setContentType("text/csv"); // Or application/vnd.quickbooks (IIF)
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.csv\"");

            importExportService.exportToQuickBooks(effectiveTenantId, startDate, endDate, response.getOutputStream());

        } catch (Exception e) {
            // Handle exception
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_FINANCE', 'IMPORT_FINANCIAL_DATA')")
    @PostMapping("/import/quickbooks")
    public ResponseEntity<Void> importFromQuickBooks(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam("file") MultipartFile file) {

        UUID effectiveTenantId = tenantResolver.resolveTenant(tenantId);
        try {
            importExportService.importFromQuickBooks(effectiveTenantId, file.getInputStream());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // Handle exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
