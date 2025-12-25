package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

// Simple DTO for a bank statement line
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.BankStatementLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReconciliationResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ReconciliationService {

    /**
     * Reconciles an internal account (e.g., Main Bank) with an imported bank statement.
     *
     * @param tenantId The tenant's ID.
     * @param accountId The internal account ID to reconcile.
     * @param statementLines A list of transactions from the bank statement.
     * @param closingBalance The closing balance as reported by the bank.
     * @return A result object detailing the reconciliation.
     */
    ReconciliationResult reconcileStatement(UUID tenantId, Long accountId, List<BankStatementLine> statementLines, BigDecimal closingBalance);
}
