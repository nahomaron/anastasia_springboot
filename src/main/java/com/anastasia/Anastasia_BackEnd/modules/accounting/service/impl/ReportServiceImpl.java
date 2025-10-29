package com.anastasia.Anastasia_BackEnd.modules.accounting.service.impl;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.*;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.LedgerEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ReportService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.util.ReportHelper;
import com.anastasia.Anastasia_BackEnd.modules.accounting.util.ReportHelper.ReportWindow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.anastasia.Anastasia_BackEnd.modules.accounting.enums.ReportType.BALANCE_SHEET;
import static com.anastasia.Anastasia_BackEnd.modules.accounting.enums.ReportType.INCOME_STATEMENT;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    @Override
    public ReportResponseDto generateReport(GenerateReportRequest request) {
        log.info("Generating report type {} for tenant {}", request.getReportType(), request.getTenantId());

        ReportWindow window = ReportHelper.resolveWindow(
                request.getPeriod(),
                request.getStartDate(),
                request.getEndDate());

        ReportResponseDto.ReportResponseDtoBuilder responseBuilder = ReportResponseDto.builder()
                .tenantId(request.getTenantId())
                .generatedAt(LocalDate.now())
                .startDate(window.startDate())
                .endDate(window.endDate());

        switch (request.getReportType()) {
            case BALANCE_SHEET:
                responseBuilder
                        .reportName("Balance Sheet")
                        .balanceSheet(buildBalanceSheet(request.getTenantId(), window));
                break;
            case INCOME_STATEMENT:
                responseBuilder
                        .reportName("Income Statement")
                        .incomeStatement(buildIncomeStatement(request.getTenantId(), window));
                break;
            default:
                throw new UnsupportedOperationException("Report type not yet implemented: " + request.getReportType());
        }

        return responseBuilder.build();
    }

    private BalanceSheetDto buildBalanceSheet(String tenantId, ReportWindow window) {
        List<Account> assetAccounts = accountRepository.findByTenantIdAndType(tenantId, AccountType.ASSET);
        List<Account> liabilityAccounts = accountRepository.findByTenantIdAndType(tenantId, AccountType.LIABILITY);
        List<Account> equityAccounts = accountRepository.findByTenantIdAndType(tenantId, AccountType.EQUITY);

        Map<Long, BigDecimal> assetAmounts = aggregateByAccount(tenantId, assetAccounts, null, window.endDate());
        Map<Long, BigDecimal> liabilityAmounts = aggregateByAccount(tenantId, liabilityAccounts, null, window.endDate());
        Map<Long, BigDecimal> equityAmounts = aggregateByAccount(tenantId, equityAccounts, null, window.endDate());

        Map<Long, BigDecimal> incomeAmountsForEquity = aggregateByAccount(tenantId,
                accountRepository.findByTenantIdAndType(tenantId, AccountType.INCOME),
                null,
                window.endDate());
        Map<Long, BigDecimal> expenseAmountsForEquity = aggregateByAccount(tenantId,
                accountRepository.findByTenantIdAndType(tenantId, AccountType.EXPENSE),
                null,
                window.endDate());

        BigDecimal netIncomeToDate = sumAmounts(incomeAmountsForEquity).subtract(sumAmounts(expenseAmountsForEquity));
        accountRepository.findByTenantIdAndCode(tenantId, "3100")
                .ifPresent(retainedEarnings -> equityAmounts.merge(retainedEarnings.getId(), netIncomeToDate, BigDecimal::add));

        BigDecimal totalAssets = sumAmounts(assetAmounts);
        BigDecimal totalLiabilities = sumAmounts(liabilityAmounts);
        BigDecimal totalEquity = sumAmounts(equityAmounts);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);

        return BalanceSheetDto.builder()
                .assets(ReportHelper.buildAccountLines(assetAccounts, assetAmounts))
                .totalAssets(totalAssets)
                .liabilities(ReportHelper.buildAccountLines(liabilityAccounts, liabilityAmounts))
                .totalLiabilities(totalLiabilities)
                .equity(ReportHelper.buildAccountLines(equityAccounts, equityAmounts))
                .totalEquity(totalEquity)
                .totalLiabilitiesAndEquity(totalLiabilitiesAndEquity)
                .isBalanced(totalAssets.compareTo(totalLiabilitiesAndEquity) == 0)
                .build();
    }

    private IncomeStatementDto buildIncomeStatement(String tenantId, ReportWindow window) {
        List<Account> incomeAccounts = accountRepository.findByTenantIdAndType(tenantId, AccountType.INCOME);
        List<Account> expenseAccounts = accountRepository.findByTenantIdAndType(tenantId, AccountType.EXPENSE);

        Map<Long, BigDecimal> incomeAmounts = aggregateByAccount(tenantId, incomeAccounts, window.startDate(), window.endDate());
        Map<Long, BigDecimal> expenseAmounts = aggregateByAccount(tenantId, expenseAccounts, window.startDate(), window.endDate());

        BigDecimal totalIncome = sumAmounts(incomeAmounts);
        BigDecimal totalExpenses = sumAmounts(expenseAmounts);

        return IncomeStatementDto.builder()
                .income(ReportHelper.buildAccountLines(incomeAccounts, incomeAmounts))
                .totalIncome(totalIncome)
                .expenses(ReportHelper.buildAccountLines(expenseAccounts, expenseAmounts))
                .totalExpenses(totalExpenses)
                .netIncome(totalIncome.subtract(totalExpenses))
                .build();
    }

    private Map<Long, BigDecimal> aggregateByAccount(String tenantId,
                                                     List<Account> accounts,
                                                     LocalDate startDate,
                                                     LocalDate endDate) {
        if (accounts.isEmpty()) {
            return Map.of();
        }

        Map<Long, Account> accountById = accounts.stream()
                .collect(Collectors.toMap(Account::getId, account -> account));

        List<AccountType> types = accounts.stream()
                .map(Account::getType)
                .distinct()
                .toList();

        Map<Long, BigDecimal> result = new HashMap<>();
        ledgerEntryRepository.aggregateByAccountAndPeriod(tenantId, types, startDate, endDate)
                .forEach(view -> {
                    Account account = accountById.get(view.getAccountId());
                    if (account == null) {
                        return;
                    }
                    BigDecimal debit = defaultAmount(view.getTotalDebit());
                    BigDecimal credit = defaultAmount(view.getTotalCredit());
                    BigDecimal amount = switch (account.getType()) {
                        case ASSET, EXPENSE -> debit.subtract(credit);
                        case LIABILITY, EQUITY, INCOME -> credit.subtract(debit);
                    };
                    result.put(account.getId(), amount);
                });

        // Ensure every account appears, even if zero.
        accounts.forEach(account -> result.putIfAbsent(account.getId(), BigDecimal.ZERO));
        return result;
    }

    private BigDecimal sumAmounts(Map<Long, BigDecimal> amounts) {
        return amounts.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
