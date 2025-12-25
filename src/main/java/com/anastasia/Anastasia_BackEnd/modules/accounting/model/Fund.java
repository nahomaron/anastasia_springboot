package com.anastasia.Anastasia_BackEnd.modules.accounting.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a designated fund (e.g., "Building Fund", "Missions Fund").
 * This is typically linked to a specific Equity account to track its balance.
 */
@Entity
@Table(name = "accounting_funds", indexes = {
        @Index(name = "idx_fund_tenant_id", columnList = "tenantId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fund extends BaseEntity {

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    /**
     * A goal amount for the fund (optional).
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal goalAmount;

    /**
     * Each fund is directly linked to a dedicated Equity account in the
     * Chart of Accounts, which holds its actual balance.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "associated_equity_account_id", referencedColumnName = "id")
    private Account associatedEquityAccount;
}
