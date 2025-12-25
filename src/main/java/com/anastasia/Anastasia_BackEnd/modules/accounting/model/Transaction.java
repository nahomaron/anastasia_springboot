package com.anastasia.Anastasia_BackEnd.modules.accounting.model;

import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single financial event (e.g., a donation, paying a bill).
 * This is the "wrapper" for the double-entry ledger.
 */
@Entity
@Table(name = "accounting_transactions", indexes = {
        @Index(name = "idx_transaction_tenant_id", columnList = "tenantId"),
        @Index(name = "idx_transaction_date", columnList = "date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "source_system")
    private String sourceSystem;

    /**
     * A transaction consists of multiple ledger entries (debits and credits).
     * The sum of debits MUST equal the sum of credits.
     */
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    // Helper method to add entries and maintain the bidirectional relationship
    public void addLedgerEntry(LedgerEntry entry) {
        ledgerEntries.add(entry);
        entry.setTransaction(this);
    }
}
