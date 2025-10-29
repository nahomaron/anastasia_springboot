package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.GenerateReportRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReportResponseDto;

public interface ReportService {

    /**
     * Generates a financial report based on the request.
     * This will dispatch to specific methods based on the report type.
     *
     * @param request The DTO containing report parameters.
     * @return A DTO containing the generated report data.
     */
    ReportResponseDto generateReport(GenerateReportRequest request);
}
