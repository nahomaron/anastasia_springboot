package com.anastasia.Anastasia_BackEnd.modules.accounting.service.impl;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.JournalEntryLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Transaction;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.TransactionRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ImportExportService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.util.QuickBooksMapper;
import com.anastasia.Anastasia_BackEnd.modules.accounting.util.QuickBooksMapper.JournalEntry;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportExportServiceImpl implements ImportExportService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public void exportToQuickBooks(UUID tenantId,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   OutputStream outputStream) {
        List<Transaction> transactions = transactionRepository.findByTenantIdAndDateBetween(tenantId, startDate, endDate);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            QuickBooksMapper.writeTransactionsAsIif(transactions, writer);
        } catch (IOException e) {
            log.error("Failed to export QuickBooks IIF for tenant={} between {} and {}", tenantId, startDate, endDate, e);
            throw new IllegalStateException("QuickBooks export failed", e);
        }
    }

    @Override
    @Transactional
    public void importFromQuickBooks(UUID tenantId, InputStream inputStream) {
        List<JournalEntry> journalEntries;
        try {
            journalEntries = QuickBooksMapper.readJournalEntries(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to parse QuickBooks IIF for tenant {}", tenantId, e);
            throw new IllegalStateException("QuickBooks import failed", e);
        }

        for (JournalEntry entry : journalEntries) {
            List<JournalEntryLine> lines = new ArrayList<>();
            for (JournalEntry.Line line : entry.getLines()) {
                Account account = resolveAccount(tenantId, line.getAccountName());
                BigDecimal amount = line.getAmount().setScale(2, RoundingMode.HALF_UP);
                BigDecimal debit = amount.compareTo(BigDecimal.ZERO) >= 0 ? amount : BigDecimal.ZERO;
                BigDecimal credit = amount.compareTo(BigDecimal.ZERO) < 0 ? amount.abs() : BigDecimal.ZERO;

                lines.add(JournalEntryLine.builder()
                        .accountId(account.getId())
                        .debit(debit)
                        .credit(credit)
                        .fundId(null)
                        .build());
            }

            ensureBalanced(lines);

            transactionService.recordJournalEntry(
                    tenantId,
                    Optional.ofNullable(entry.getDate()).orElse(LocalDate.now()),
                    Optional.ofNullable(entry.getMemo()).filter(s -> !s.isBlank()).orElse("Imported QuickBooks Entry"),
                    lines);
        }
    }

    private Account resolveAccount(UUID tenantId, String accountName) {
        return accountRepository.findByTenantIdAndNameIgnoreCase(tenantId, accountName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account in QuickBooks import: " + accountName));
    }

    private void ensureBalanced(List<JournalEntryLine> lines) {
        BigDecimal totalDebit = lines.stream()
                .map(JournalEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream()
                .map(JournalEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.subtract(totalCredit).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("Imported QuickBooks entry is not balanced: debits=" + totalDebit + " credits=" + totalCredit);
        }
    }
}
