package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

/**
 * Manages the creation and management of the Chart of Accounts (CoA).
 */
public interface ChartOfAccountsService {

    /**
     * Creates the default set of accounts for a new tenant.
     * This is a critical first step for any new tenant.
     *
     * @param tenantId The unique ID of the tenant.
     */
    void createInitialChartOfAccounts(java.util.UUID tenantId);
}
