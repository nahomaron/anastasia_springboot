package com.anastasia.Anastasia_BackEnd.modules.accounting.model;

import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an account in the Chart of Accounts.
 * This is the core ledger where all balances are stored.
 */
@Entity
@Table(name = "accounting_accounts", indexes = {
        @Index(name = "idx_account_tenant_id", columnList = "tenantId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    /**
     * A unique code for this account (e.g., "1010" for Cash, "4010" for Tithes).
     */
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType type;

    private String description;

    /**
     * The current balance of the account. This is updated by every transaction.
     * ALWAYS use BigDecimal for financial calculations.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /**
     * Self-referencing relationship to create a hierarchical chart of accounts.
     * (e.g., "Checking" and "Savings" are children of "Bank Accounts").
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_account_id")
    private Account parentAccount;

    @OneToMany(mappedBy = "parentAccount")
    private List<Account> childAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();
}
