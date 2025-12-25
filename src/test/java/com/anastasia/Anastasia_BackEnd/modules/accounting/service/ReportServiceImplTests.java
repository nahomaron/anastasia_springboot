package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.GenerateReportRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.JournalEntryLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReportResponseDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.ReportPeriod;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.ReportType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReportServiceImplTests {

    @Autowired
    private ReportService reportService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ChartOfAccountsService chartOfAccountsService;

    @Autowired
    private AccountRepository accountRepository;

    private UUID tenantId;
    private Account bankAccount;
    private Account donationIncome;
    private Account processingFees;

    @BeforeEach
    void init() {
        tenantId = UUID.randomUUID();
        chartOfAccountsService.createInitialChartOfAccounts(tenantId);
        bankAccount = accountRepository.findByTenantIdAndCode(tenantId, "1110").orElseThrow();
        donationIncome = accountRepository.findByTenantIdAndCode(tenantId, "4200").orElseThrow();
        processingFees = accountRepository.findByTenantIdAndCode(tenantId, "5210").orElseThrow();

        transactionService.recordJournalEntry(tenantId, LocalDate.now(), "Receive donation",
                List.of(
                        JournalEntryLine.builder()
                                .accountId(bankAccount.getId())
                                .debit(new BigDecimal("200.00"))
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryLine.builder()
                                .accountId(donationIncome.getId())
                                .debit(BigDecimal.ZERO)
                                .credit(new BigDecimal("200.00"))
                                .build()
                ));

        transactionService.recordJournalEntry(tenantId, LocalDate.now(), "Processor fee",
                List.of(
                        JournalEntryLine.builder()
                                .accountId(processingFees.getId())
                                .debit(new BigDecimal("50.00"))
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryLine.builder()
                                .accountId(bankAccount.getId())
                                .debit(BigDecimal.ZERO)
                                .credit(new BigDecimal("50.00"))
                                .build()
                ));
    }

    @Test
    void incomeStatementReflectsPeriodActivity() {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setTenantId(tenantId);
        request.setReportType(ReportType.INCOME_STATEMENT);
        request.setPeriod(ReportPeriod.CUSTOM);
        request.setStartDate(LocalDate.now().minusDays(1));
        request.setEndDate(LocalDate.now());

        ReportResponseDto report = reportService.generateReport(request);

        assertThat(report.getIncomeStatement().getTotalIncome()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(report.getIncomeStatement().getTotalExpenses()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(report.getIncomeStatement().getNetIncome()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void balanceSheetBalancesAssetsAndEquity() {
        GenerateReportRequest request = new GenerateReportRequest();
        request.setTenantId(tenantId);
        request.setReportType(ReportType.BALANCE_SHEET);
        request.setPeriod(ReportPeriod.CUSTOM);
        request.setStartDate(LocalDate.now().minusDays(1));
        request.setEndDate(LocalDate.now());

        ReportResponseDto report = reportService.generateReport(request);

        assertThat(report.getBalanceSheet().getTotalAssets()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(report.getBalanceSheet().getTotalLiabilitiesAndEquity()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(report.getBalanceSheet().isBalanced()).isTrue();
    }
}
