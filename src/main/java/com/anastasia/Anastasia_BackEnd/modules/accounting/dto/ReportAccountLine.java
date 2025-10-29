package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single line item in a report.
 */
@Data
@Builder
public class ReportAccountLine {
    private String accountName;
    private String accountCode;
    private BigDecimal amount;
    @Builder.Default
    private List<ReportAccountLine> children = List.of(); // For hierarchical reports
}
