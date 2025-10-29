package com.anastasia.Anastasia_BackEnd.modules.accounting.model;


import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Fund;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents a single line item in the accounting ledger (a debit or a credit).
 * This is the core of the double-entry system.
 */
@Entity
@Table(name = "accounting_ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_id")
    private Fund fund;

    /**
     * The debit amount. For a given entry, either debit or credit must be zero.
     * Debits increase Assets and Expenses.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal debit;

    /**
     * The credit amount.
     * Credits increase Liabilities, Equity, and Income.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal credit;
}
