package com.anastasia.Anastasia_BackEnd.modules.accounting.service.impl;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.TransactionType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.InsufficientFundsException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.InvalidTransactionException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.ResourceNotFoundException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Fund;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.LedgerEntry;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Transaction;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.TransactionRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.PaymentAccountingIntegrationService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private static final ZoneId DEFAULT_TIME_ZONE = ZoneId.of("UTC");
    private static final String SOURCE_PAYMENTS = "PAYMENTS";

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentAccountingIntegrationService paymentIntegrationService;
    private final FundRepository fundRepository;

    @Override
    @Transactional
    public TransactionDto recordIncome(RecordIncomeRequest request) {
        log.info("Recording income for tenant: {}", request.getTenantId());

        // 1. Get Accounts
        Account assetAccount = getAccountForUpdate(request.getAssetAccountId(), request.getTenantId());
        Account incomeAccount = getAccountForUpdate(request.getIncomeAccountId(), request.getTenantId());

        // 2. Validate Accounts
        if (assetAccount.getType() != ASSET) {
            throw new InvalidTransactionException("Asset account is not of type ASSET.");
        }
        if (incomeAccount.getType() != INCOME) {
            throw new InvalidTransactionException("Income account is not of type INCOME.");
        }

        // 3. Create Transaction Wrapper
        Transaction transaction = Transaction.builder()
                .tenantId(request.getTenantId())
                .date(request.getDate())
                .description(request.getDescription())
                .type(TransactionType.INCOME)
                .build();

        // 4. Create Ledger Entries (The Double-Entry part)
        // Debit: Increase the Asset account
        LedgerEntry assetEntry = LedgerEntry.builder()
                .account(assetAccount)
                .debit(request.getAmount())
                .credit(BigDecimal.ZERO)
                .build();

        // Credit: Increase the Income account
        LedgerEntry incomeEntry = LedgerEntry.builder()
                .account(incomeAccount)
                .debit(BigDecimal.ZERO)
                .credit(request.getAmount())
                .build();

        transaction.addLedgerEntry(assetEntry);
        transaction.addLedgerEntry(incomeEntry);

        // 5. Post entries and update balances
        postTransaction(transaction);

        return toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDto recordExpense(RecordExpenseRequest request) {
        log.info("Recording expense for tenant: {}", request.getTenantId());

        // 1. Get Accounts
        Account assetAccount = getAccountForUpdate(request.getAssetAccountId(), request.getTenantId());
        Account expenseAccount = getAccountForUpdate(request.getExpenseAccountId(), request.getTenantId());

        // 2. Validate Accounts
        if (assetAccount.getType() != ASSET) {
            throw new InvalidTransactionException("Asset account is not of type ASSET.");
        }
        if (expenseAccount.getType() != EXPENSE) {
            throw new InvalidTransactionException("Expense account is not of type EXPENSE.");
        }

        // 3. Check for sufficient funds
        if (assetAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account: " + assetAccount.getName());
        }

        // 4. Create Transaction Wrapper
        Transaction transaction = Transaction.builder()
                .tenantId(request.getTenantId())
                .date(request.getDate())
                .description(request.getDescription())
                .type(TransactionType.EXPENSE)
                .build();

        // 5. Create Ledger Entries
        // Debit: Increase the Expense account
        LedgerEntry expenseEntry = LedgerEntry.builder()
                .account(expenseAccount)
                .debit(request.getAmount())
                .credit(BigDecimal.ZERO)
                .build();

        // Credit: Decrease the Asset account
        LedgerEntry assetEntry = LedgerEntry.builder()
                .account(assetAccount)
                .debit(BigDecimal.ZERO)
                .credit(request.getAmount())
                .build();

        transaction.addLedgerEntry(expenseEntry);
        transaction.addLedgerEntry(assetEntry);

        // 6. Post entries and update balances
        postTransaction(transaction);

        return toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDto transferFunds(TransferFundsRequest request) {
        log.info("Transferring funds for tenant: {}", request.getTenantId());

        if (request.getFromAssetAccountId().equals(request.getToAssetAccountId())) {
            throw new InvalidTransactionException("Cannot transfer to and from the same account.");
        }

        // 1. Get Accounts
        Account fromAccount = getAccountForUpdate(request.getFromAssetAccountId(), request.getTenantId());
        Account toAccount = getAccountForUpdate(request.getToAssetAccountId(), request.getTenantId());

        // 2. Validate
        if (fromAccount.getType() != ASSET || toAccount.getType() != ASSET) {
            throw new InvalidTransactionException("Both accounts must be of type ASSET for a transfer.");
        }

        // 3. Check for sufficient funds
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account: " + fromAccount.getName());
        }

        // 4. Create Transaction Wrapper
        Transaction transaction = Transaction.builder()
                .tenantId(request.getTenantId())
                .date(request.getDate())
                .description(request.getDescription())
                .type(TransactionType.TRANSFER)
                .build();

        // 5. Create Ledger Entries
        // Debit: Increase the 'To' Asset account
        LedgerEntry toEntry = LedgerEntry.builder()
                .account(toAccount)
                .debit(request.getAmount())
                .credit(BigDecimal.ZERO)
                .build();

        // Credit: Decrease the 'From' Asset account
        LedgerEntry fromEntry = LedgerEntry.builder()
                .account(fromAccount)
                .debit(BigDecimal.ZERO)
                .credit(request.getAmount())
                .build();

        transaction.addLedgerEntry(toEntry);
        transaction.addLedgerEntry(fromEntry);

        // 6. Post entries and update balances
        postTransaction(transaction);

        return toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDto recordPaymentCapture(PaymentCapturedMessage message) {
        PaymentPurpose purpose = parsePurpose(message.getPurpose());
        log.info("Posting payment capture to ledger. tenant={} paymentId={} purpose={}",
                message.getTenantId(), message.getPaymentId(), purpose);

        var existing = transactionRepository.findByTenantIdAndExternalReferenceAndSourceSystem(
                message.getTenantId(), message.getPaymentId(), SOURCE_PAYMENTS);
        if (existing.isPresent()) {
            log.info("Payment capture already posted (transactionId={}). Skipping replay.", existing.get().getId());
            return toDto(existing.get());
        }

        BigDecimal gross = toMajor(message.getGrossAmountMinor(), message.getCurrency());
        BigDecimal fees = toMajor(message.getFeeAmountMinor(), message.getCurrency());
        BigDecimal net = alignNetAmount(gross, toMajor(message.getNetAmountMinor(), message.getCurrency()), fees);

        validatePaymentAmounts(gross, net, fees);

        PaymentAccountingIntegrationService.AccountingMapping mapping =
                paymentIntegrationService.resolve(message.getTenantId(), purpose, message.getFundId());

        Account assetAccount = getAccountForUpdate(mapping.getAssetAccount().getId(), message.getTenantId());
        Account incomeAccount = getAccountForUpdate(mapping.getIncomeAccount().getId(), message.getTenantId());
        Account feeAccount = null;
        if (fees.compareTo(BigDecimal.ZERO) > 0) {
            if (mapping.getFeeAccount() == null) {
                throw new ResourceNotFoundException("No processing fee account configured for tenant");
            }
            feeAccount = getAccountForUpdate(mapping.getFeeAccount().getId(), message.getTenantId());
        }

        Transaction transaction = Transaction.builder()
                .tenantId(message.getTenantId())
                .date(resolveTransactionDate(message.getCapturedAt()))
                .description(buildPaymentDescription(purpose, message.getPaymentId()))
                .type(TransactionType.INCOME)
                .externalReference(message.getPaymentId())
                .sourceSystem(SOURCE_PAYMENTS)
                .build();

        LedgerEntry assetEntry = LedgerEntry.builder()
                .account(assetAccount)
                .debit(net)
                .credit(BigDecimal.ZERO)
                .build();

        transaction.addLedgerEntry(assetEntry);

        if (fees.compareTo(BigDecimal.ZERO) > 0 && feeAccount != null) {
            LedgerEntry feeEntry = LedgerEntry.builder()
                    .account(feeAccount)
                    .debit(fees)
                    .credit(BigDecimal.ZERO)
                    .build();
            transaction.addLedgerEntry(feeEntry);
        }

        LedgerEntry incomeEntry = LedgerEntry.builder()
                .account(incomeAccount)
                .debit(BigDecimal.ZERO)
                .credit(gross)
                .fund(mapping.getFund())
                .build();
        transaction.addLedgerEntry(incomeEntry);

        postTransaction(transaction);
        return toDto(transaction);
    }

    @Override
    @Transactional
    public TransactionDto recordJournalEntry(UUID tenantId,
                                             LocalDate date,
                                             String description,
                                             java.util.List<JournalEntryLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidTransactionException("Journal entry must contain at least one line");
        }

        Transaction transaction = Transaction.builder()
                .tenantId(tenantId)
                .date(date)
                .description(description)
                .type(TransactionType.TRANSFER)
                .build();

        for (JournalEntryLine line : lines) {
            Account account = getAccountForUpdate(line.getAccountId(), tenantId);
            BigDecimal debit = normalizeAmount(line.getDebit());
            BigDecimal credit = normalizeAmount(line.getCredit());

            if (debit.signum() < 0 || credit.signum() < 0) {
                throw new InvalidTransactionException("Debit and credit values must be positive");
            }
            if ((debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0)
                    || (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0)) {
                throw new InvalidTransactionException("Each journal line must have either a debit or a credit amount");
            }

            Fund fund = resolveFund(line.getFundId(), tenantId);

            LedgerEntry entry = LedgerEntry.builder()
                    .account(account)
                    .debit(debit)
                    .credit(credit)
                    .fund(fund)
                    .build();
            transaction.addLedgerEntry(entry);
        }

        postTransaction(transaction);
        return toDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long transactionId, UUID tenantId) {
        Transaction transaction = transactionRepository.findByIdAndTenantId(transactionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + transactionId));
        transaction.getLedgerEntries().size();
        return toDto(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactions(UUID tenantId, LocalDate startDate, LocalDate endDate, Long accountId) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be on or before end date");
        }

        return transactionRepository.findVisibleTransactions(tenantId, startDate, endDate, accountId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * This private method contains the core logic for validating and saving
     * a transaction and its entries, and updating all affected account balances.
     * It's marked @Transactional, so if any part fails, the entire
     * operation rolls back.
     */
    @Transactional
    private void postTransaction(Transaction transaction) {
        // 1. Validate Transaction (Debits must equal Credits)
        validateTransaction(transaction);

        // 2. Save the transaction and its ledger entries
        transactionRepository.save(transaction);

        // 3. Update account balances
        for (LedgerEntry entry : transaction.getLedgerEntries()) {
            updateAccountBalance(entry);
        }
        log.info("Successfully posted transaction: {}", transaction.getId());
    }

    /**
     * Updates the balance of a single account based on a ledger entry.
     * This is the core of the accounting logic.
     */
    private void updateAccountBalance(LedgerEntry entry) {
        Account account = entry.getAccount(); // This account is already locked
        BigDecimal debit = entry.getDebit();
        BigDecimal credit = entry.getCredit();

        BigDecimal newBalance;

        // The fundamental accounting equation logic
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                // Debits increase, Credits decrease
                newBalance = account.getBalance().add(debit).subtract(credit);
                break;

            case LIABILITY:
            case EQUITY:
            case INCOME:
                // Credits increase, Debits decrease
                newBalance = account.getBalance().subtract(debit).add(credit);
                break;

            default:
                throw new InvalidTransactionException("Unknown account type: " + account.getType());
        }

        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    /**
     * Ensures that the total debits equal the total credits for the transaction.
     */
    private void validateTransaction(Transaction transaction) {
        BigDecimal totalDebit = transaction.getLedgerEntries().stream()
                .map(LedgerEntry::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = transaction.getLedgerEntries().stream()
                .map(LedgerEntry::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            log.error("Transaction is unbalanced. Debits: {}, Credits: {}", totalDebit, totalCredit);
            throw new InvalidTransactionException("Transaction is unbalanced. Debits must equal credits.");
        }
    }

    /**
     * Fetches an account and locks it for an update to prevent race conditions.
     */
    private Account getAccountForUpdate(Long accountId, UUID tenantId) {
        return accountRepository.findByIdAndTenantIdForUpdate(accountId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    @Override
    public TransactionDto toDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .date(transaction.getDate())
                .description(transaction.getDescription())
                .type(transaction.getType())
                .externalReference(transaction.getExternalReference())
                .sourceSystem(transaction.getSourceSystem())
                .ledgerEntries(transaction.getLedgerEntries().stream()
                        .map(this::toLedgerDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private LedgerEntryDto toLedgerDto(LedgerEntry entry) {
        return LedgerEntryDto.builder()
                .id(entry.getId())
                .accountId(entry.getAccount().getId())
                .accountName(entry.getAccount().getName())
                .fundId(entry.getFund() != null ? entry.getFund().getId() : null)
                .fundName(entry.getFund() != null ? entry.getFund().getName() : null)
                .debit(entry.getDebit())
                .credit(entry.getCredit())
                .build();
    }

    private PaymentPurpose parsePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return PaymentPurpose.OTHER;
        }
        try {
            return PaymentPurpose.valueOf(purpose);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown payment purpose '{}' received, defaulting to OTHER", purpose);
            return PaymentPurpose.OTHER;
        }
    }

    private BigDecimal toMajor(Long amountMinor, String currencyCode) {
        if (amountMinor == null) {
            return BigDecimal.ZERO;
        }

        int scale = 2;
        if (currencyCode != null && !currencyCode.isBlank()) {
            try {
                scale = Currency.getInstance(currencyCode).getDefaultFractionDigits();
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown currency code '{}' while converting payment amounts. Falling back to 2 decimals.",
                        currencyCode);
            }
        }

        return BigDecimal.valueOf(amountMinor, Math.max(scale, 0));
    }

    private BigDecimal alignNetAmount(BigDecimal gross, BigDecimal net, BigDecimal fees) {
        BigDecimal expectedNet = gross.subtract(fees);
        if (net == null) {
            return expectedNet;
        }

        BigDecimal tolerance = computeTolerance(gross, net, fees);
        if (expectedNet.subtract(net).abs().compareTo(tolerance) > 0) {
            log.warn("Payment net amount mismatch. gross={} fees={} net={} (adjusted to {})", gross, fees, net,
                    expectedNet);
            return expectedNet;
        }
        return net;
    }

    private void validatePaymentAmounts(BigDecimal gross, BigDecimal net, BigDecimal fees) {
        if (gross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Captured payment must have a positive gross amount");
        }
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Captured payment net amount cannot be negative");
        }
        if (fees.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransactionException("Captured payment fee amount cannot be negative");
        }

        BigDecimal difference = gross.subtract(net.add(fees)).abs();
        BigDecimal tolerance = computeTolerance(gross, net, fees);
        if (difference.compareTo(tolerance) > 0) {
            throw new InvalidTransactionException("Captured payment amounts are imbalanced (gross != net + fees)");
        }
    }

    private BigDecimal computeTolerance(BigDecimal gross, BigDecimal net, BigDecimal fees) {
        int scale = Math.max(gross.scale(), Math.max(net.scale(), fees.scale()));
        if (scale <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.movePointLeft(scale);
    }

    private LocalDate resolveTransactionDate(Instant capturedAt) {
        if (capturedAt == null) {
            return LocalDate.now(DEFAULT_TIME_ZONE);
        }
        return capturedAt.atZone(DEFAULT_TIME_ZONE).toLocalDate();
    }

    private String buildPaymentDescription(PaymentPurpose purpose, String paymentId) {
        String base = "Captured " + purpose.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        if (paymentId != null) {
            return base + " (payment " + paymentId + ")";
        }
        return base;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Fund resolveFund(Long fundId, UUID tenantId) {
        if (fundId == null) {
            return null;
        }
        return fundRepository.findByIdAndTenantId(fundId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + fundId));
    }
}
