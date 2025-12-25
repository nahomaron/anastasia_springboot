package com.anastasia.Anastasia_BackEnd.modules.accounting.controller;


import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ImportExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounting/io")
@RequiredArgsConstructor
public class ImportExportController {

    private final ImportExportService importExportService;

    @GetMapping("/export/quickbooks")
    public void exportToQuickBooks(
            @RequestParam UUID tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response) {

        try {
            response.setContentType("text/csv"); // Or application/vnd.quickbooks (IIF)
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.csv\"");

            importExportService.exportToQuickBooks(tenantId, startDate, endDate, response.getOutputStream());

        } catch (Exception e) {
            // Handle exception
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @PostMapping("/import/quickbooks")
    public ResponseEntity<Void> importFromQuickBooks(
            @RequestParam UUID tenantId,
            @RequestParam("file") MultipartFile file) {

        try {
            importExportService.importFromQuickBooks(tenantId, file.getInputStream());
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            // Handle exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
