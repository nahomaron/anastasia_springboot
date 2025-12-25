package com.anastasia.Anastasia_BackEnd.modules.accounting.service.impl;


import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ChartOfAccountsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartOfAccountsServiceImpl implements ChartOfAccountsService {

    private final AccountRepository accountRepository;

    /**
     * This method is @Transactional to ensure that all default accounts are
     * created successfully, or none are.
     */
    @Override
    @Transactional
    public void createInitialChartOfAccounts(UUID tenantId) {
        log.info("Creating initial Chart of Accounts for tenantId: {}", tenantId);

        // 1. Assets (1000 series)
        Account assets = createAccount(tenantId, "Assets", "1000", AccountType.ASSET, null);
        Account currentAssets = createAccount(tenantId, "Current Assets", "1100", AccountType.ASSET, assets);
        createAccount(tenantId, "Main Bank Account", "1110", AccountType.ASSET, currentAssets);
        createAccount(tenantId, "Cash on Hand", "1120", AccountType.ASSET, currentAssets);

        // 2. Liabilities (2000 series)
        Account liabilities = createAccount(tenantId, "Liabilities", "2000", AccountType.LIABILITY, null);
        Account currentLiabilities = createAccount(tenantId, "Current Liabilities", "2100", AccountType.LIABILITY, liabilities);
        createAccount(tenantId, "Credit Card", "2110", AccountType.LIABILITY, currentLiabilities);

        // 3. Equity (3000 series)
        Account equity = createAccount(tenantId, "Equity", "3000", AccountType.EQUITY, null);
        createAccount(tenantId, "Retained Earnings", "3100", AccountType.EQUITY, equity);
        createAccount(tenantId, "General Fund", "3200", AccountType.EQUITY, equity); // Default fund

        // 4. Income (4000 series)
        Account income = createAccount(tenantId, "Income", "4000", AccountType.INCOME, null);
        createAccount(tenantId, "Tithe Income", "4100", AccountType.INCOME, income);
        createAccount(tenantId, "Donations", "4200", AccountType.INCOME, income);
        createAccount(tenantId, "Certificate Payments", "4300", AccountType.INCOME, income);
        createAccount(tenantId, "Subscription Revenue", "4400", AccountType.INCOME, income);
        createAccount(tenantId, "Bookstore / Store Sales", "4500", AccountType.INCOME, income);
        createAccount(tenantId, "Sunday School Donations", "4600", AccountType.INCOME, income);
        createAccount(tenantId, "Special Event Receipts", "4700", AccountType.INCOME, income);
        createAccount(tenantId, "Miscellaneous Income", "4990", AccountType.INCOME, income);

        // 5. Expenses (5000 series)
        Account expenses = createAccount(tenantId, "Expenses", "5000", AccountType.EXPENSE, null);
        createAccount(tenantId, "Employee Salaries", "5100", AccountType.EXPENSE, expenses);
        createAccount(tenantId, "Office Expenses", "5200", AccountType.EXPENSE, expenses);
        createAccount(tenantId, "Payment Processing Fees", "5210", AccountType.EXPENSE, expenses);
        createAccount(tenantId, "Church Expenses", "5300", AccountType.EXPENSE, expenses);

        log.info("Successfully created Chart of Accounts for tenantId: {}", tenantId);
    }

    private Account createAccount(UUID tenantId, String name, String code, AccountType type, Account parent) {
        return accountRepository.findByTenantIdAndCode(tenantId, code)
                .orElseGet(() -> doCreateAccount(tenantId, name, code, type, parent));
    }

    private Account doCreateAccount(UUID tenantId, String name, String code, AccountType type, Account parent) {
        Account account = Account.builder()
                .tenantId(tenantId)
                .name(name)
                .code(code)
                .type(type)
                .balance(BigDecimal.ZERO)
                .parentAccount(parent)
                .build();
        return accountRepository.save(account);
    }
}
