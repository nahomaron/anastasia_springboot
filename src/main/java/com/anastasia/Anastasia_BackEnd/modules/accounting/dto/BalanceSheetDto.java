package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BalanceSheetDto {
    private List<ReportAccountLine> assets;
    private BigDecimal totalAssets;
    private List<ReportAccountLine> liabilities;
    private BigDecimal totalLiabilities;
    private List<ReportAccountLine> equity;
    private BigDecimal totalEquity;
    private BigDecimal totalLiabilitiesAndEquity;
    private boolean isBalanced;
}
