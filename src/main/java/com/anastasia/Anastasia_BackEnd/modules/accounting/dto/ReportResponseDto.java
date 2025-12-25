package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A generic container for different report types.
 */
@Data
@Builder
public class ReportResponseDto {
    private UUID tenantId;
    private String reportName;
    private LocalDate generatedAt;
    private LocalDate startDate;
    private LocalDate endDate;

    // Use specific DTOs for each report type
    private BalanceSheetDto balanceSheet;
    private IncomeStatementDto incomeStatement;
    // Add other report DTOs here (e.g., CashFlowStatementDto)
}
