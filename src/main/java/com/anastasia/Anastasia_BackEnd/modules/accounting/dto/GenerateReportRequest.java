package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;


import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.ReportPeriod;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class GenerateReportRequest {
    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Report period is required")
    private ReportPeriod period;

    // Only required if period is CUSTOM
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
