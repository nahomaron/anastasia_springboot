package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class IncomeStatementDto {
    private List<ReportAccountLine> income;
    private BigDecimal totalIncome;
    private List<ReportAccountLine> expenses;
    private BigDecimal totalExpenses;
    private BigDecimal netIncome;
}
