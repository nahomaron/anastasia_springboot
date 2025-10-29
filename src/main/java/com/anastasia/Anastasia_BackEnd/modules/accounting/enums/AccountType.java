package com.anastasia.Anastasia_BackEnd.modules.accounting.enums;

/**
 * Represents the fundamental accounting equation: Assets = Liabilities + Equity.
 * Income and Expenses are temporary accounts that roll up into Equity.
 */
public enum AccountType {
    ASSET,      // What the church owns (e.g., Bank Account, Cash, Buildings)
    LIABILITY,  // What the church owes (e.g., Loans, Credit Card Debt)
    EQUITY,     // The net worth (e.g., Retained Earnings, Fund Balances)
    INCOME,     // Revenue sources (e.g., Tithes, Donations, Offerings)
    EXPENSE     // Costs incurred (e.g., Salaries, Utilities, Rent)
}