package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Simple DTO for the reconciliation result
@Data
public class ReconciliationResult {
    private BigDecimal closingBankBalance;
    private BigDecimal internalBookBalance;
    private BigDecimal adjustedBankBalance;
    private BigDecimal adjustedBookBalance;
    private BigDecimal outstandingTotal;
    private BigDecimal unrecordedTotal;
    private BigDecimal difference;
    private List<String> outstandingTransactions;
    private List<String> unrecordedBankItems;
}
