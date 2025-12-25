package com.anastasia.Anastasia_BackEnd.modules.accounting.service;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.JournalEntryLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.PaymentCapturedMessage;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordExpenseRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordIncomeRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransactionDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransferFundsRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Transaction;

public interface TransactionService {

    /**
     * Records an income transaction (e.g., tithe, donation).
     * This will create a debit to an Asset account (e.g., Bank) and
     * a credit to an Income account (e.g., Tithes).
     */
    TransactionDto recordIncome(RecordIncomeRequest request);

    /**
     * Records an expense transaction (e.g., salary, utilities).
     * This will create a debit to an Expense account (e.g., Salaries) and
     * a credit to an Asset account (e.g., Bank).
     */
    TransactionDto recordExpense(RecordExpenseRequest request);

    /**
     * Records a transfer of funds between two asset accounts.
     * This will create a debit to one Asset account (e.g., Bank) and
     * a credit to another Asset account (e.g., Cash on Hand).
     */
    TransactionDto transferFunds(TransferFundsRequest request);

    /**
     * Records the accounting impact of a captured payment event coming from the payment service.
     */
    TransactionDto recordPaymentCapture(PaymentCapturedMessage message);

    /**
     * Records a general journal entry comprised of arbitrary debit and credit lines.
     */
    TransactionDto recordJournalEntry(java.util.UUID tenantId, java.time.LocalDate date, String description, java.util.List<JournalEntryLine> lines);

    // Helper to convert entity to DTO
    TransactionDto toDto(Transaction transaction);
}
