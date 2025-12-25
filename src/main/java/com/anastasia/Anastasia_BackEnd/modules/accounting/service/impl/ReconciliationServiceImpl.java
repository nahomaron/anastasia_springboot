package com.anastasia.Anastasia_BackEnd.modules.accounting.service.impl;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.BankStatementLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReconciliationResult;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.ResourceNotFoundException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.LedgerEntry;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.LedgerEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.LedgerEntryRepository.AccountBalanceView;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ReconciliationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final BigDecimal MATCH_TOLERANCE = new BigDecimal("0.01");
    private static final long DATE_TOLERANCE_DAYS = 3;

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Override
    @Transactional(readOnly = true)
    public ReconciliationResult reconcileStatement(UUID tenantId,
                                                   Long accountId,
                                                   List<BankStatementLine> statementLines,
                                                   BigDecimal closingBalance) {

        if (statementLines == null) {
            statementLines = List.of();
        }

        BigDecimal effectiveClosingBalance = closingBalance == null ? BigDecimal.ZERO : closingBalance;

        Account account = accountRepository.findByIdAndTenantId(accountId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        LocalDate statementStart = statementLines.stream()
                .map(BankStatementLine::getDate)
                .filter(date -> date != null)
                .min(Comparator.naturalOrder())
                .orElse(LocalDate.now().minusMonths(1));

        LocalDate statementEnd = statementLines.stream()
                .map(BankStatementLine::getDate)
                .filter(date -> date != null)
                .max(Comparator.naturalOrder())
                .orElse(LocalDate.now());

        List<LedgerCandidate> ledgerCandidates = ledgerEntryRepository
                .findByAccountAndDateRange(accountId, statementStart.minusDays(30), statementEnd)
                .stream()
                .map(entry -> new LedgerCandidate(entry, account.getType()))
                .collect(Collectors.toCollection(ArrayList::new));

        List<String> unrecordedBankItems = new ArrayList<>();
        BigDecimal unrecordedTotal = BigDecimal.ZERO;

        for (BankStatementLine line : statementLines) {
            BigDecimal statementAmount = sanitizeAmount(line.getAmount());
            LocalDate lineDate = Optional.ofNullable(line.getDate()).orElse(statementEnd);

            LedgerCandidate match = matchLedgerEntry(ledgerCandidates, statementAmount, lineDate);
            if (match != null) {
                ledgerCandidates.remove(match);
            } else {
                unrecordedBankItems.add(formatStatementLine(line));
                unrecordedTotal = unrecordedTotal.add(statementAmount);
            }
        }

        List<String> outstandingTransactions = new ArrayList<>();
        BigDecimal outstandingTotal = BigDecimal.ZERO;
        for (LedgerCandidate remaining : ledgerCandidates) {
            outstandingTransactions.add(formatLedgerEntry(remaining.entry));
            outstandingTotal = outstandingTotal.add(remaining.amount);
        }

        BigDecimal internalBookBalance = calculateBalanceAsOf(account, statementEnd);
        BigDecimal adjustedBookBalance = internalBookBalance.add(unrecordedTotal);
        BigDecimal adjustedBankBalance = effectiveClosingBalance.add(outstandingTotal);
        BigDecimal difference = adjustedBookBalance.subtract(adjustedBankBalance);

        ReconciliationResult result = new ReconciliationResult();
        result.setClosingBankBalance(effectiveClosingBalance);
        result.setInternalBookBalance(internalBookBalance);
        result.setAdjustedBookBalance(adjustedBookBalance);
        result.setAdjustedBankBalance(adjustedBankBalance);
        result.setOutstandingTotal(outstandingTotal);
        result.setUnrecordedTotal(unrecordedTotal);
        result.setDifference(difference);
        result.setOutstandingTransactions(outstandingTransactions);
        result.setUnrecordedBankItems(unrecordedBankItems);
        return result;
    }

    private LedgerCandidate matchLedgerEntry(List<LedgerCandidate> candidates, BigDecimal amount, LocalDate date) {
        if (candidates.isEmpty()) {
            return null;
        }
        for (Iterator<LedgerCandidate> iterator = candidates.iterator(); iterator.hasNext(); ) {
            LedgerCandidate candidate = iterator.next();
            if (amountMatches(candidate.amount, amount) && dateMatches(candidate.date, date)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean amountMatches(BigDecimal ledgerAmount, BigDecimal statementAmount) {
        return ledgerAmount.subtract(statementAmount).abs().compareTo(MATCH_TOLERANCE) <= 0;
    }

    private boolean dateMatches(LocalDate ledgerDate, LocalDate statementDate) {
        long days = Math.abs(ChronoUnit.DAYS.between(ledgerDate, statementDate));
        return days <= DATE_TOLERANCE_DAYS;
    }

    private BigDecimal calculateBalanceAsOf(Account account, LocalDate endDate) {
        List<AccountBalanceView> aggregates = ledgerEntryRepository.aggregateByAccountAndPeriod(
                account.getTenantId(),
                List.of(account.getType()),
                null,
                endDate);

        return aggregates.stream()
                .filter(view -> view.getAccountId().equals(account.getId()))
                .findFirst()
                .map(view -> {
                    BigDecimal debit = sanitizeAmount(view.getTotalDebit());
                    BigDecimal credit = sanitizeAmount(view.getTotalCredit());
                    return switch (account.getType()) {
                        case ASSET, EXPENSE -> debit.subtract(credit);
                        case LIABILITY, EQUITY, INCOME -> credit.subtract(debit);
                    };
                })
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal sanitizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatStatementLine(BankStatementLine line) {
        return String.format("%s | %s | %s",
                Optional.ofNullable(line.getDate()).map(LocalDate::toString).orElse("-"),
                Optional.ofNullable(line.getDescription()).orElse("(No description)"),
                sanitizeAmount(line.getAmount()));
    }

    private String formatLedgerEntry(LedgerEntry entry) {
        BigDecimal amount = entry.getDebit().subtract(entry.getCredit()).setScale(2, RoundingMode.HALF_UP);
        return String.format("%s | %s | %s",
                entry.getTransaction().getDate(),
                entry.getTransaction().getDescription(),
                amount);
    }

    private static class LedgerCandidate {
        private final LedgerEntry entry;
        private final BigDecimal amount;
        private final LocalDate date;

        private LedgerCandidate(LedgerEntry entry, AccountType accountType) {
            this.entry = entry;
            BigDecimal debit = entry.getDebit();
            BigDecimal credit = entry.getCredit();
            BigDecimal rawAmount = switch (accountType) {
                case ASSET, EXPENSE -> debit.subtract(credit);
                case LIABILITY, EQUITY, INCOME -> credit.subtract(debit);
            };
            this.amount = rawAmount.setScale(2, RoundingMode.HALF_UP);
            this.date = entry.getTransaction().getDate();
        }
    }
}
