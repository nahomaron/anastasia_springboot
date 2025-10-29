package com.anastasia.Anastasia_BackEnd.modules.accounting.enums;

/**
 * High-level categorization of a transaction for easier reporting.
 */
public enum TransactionType {
    INCOME,     // e.g., Tithe, Donation
    EXPENSE,    // e.g., Salary, Utility Bill
    TRANSFER    // e.g., Moving cash to the bank
}