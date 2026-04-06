package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.BankStatementLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.JournalEntryLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.ReconciliationResult;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Tag("experimental")
class ReconciliationServiceImplTests {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ChartOfAccountsService chartOfAccountsService;

    @Autowired
    private AccountRepository accountRepository;

    private UUID tenantId;
    private Account bank;
    private Account donationIncome;
    private Account expenseAccount;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        chartOfAccountsService.createInitialChartOfAccounts(tenantId);
        bank = accountRepository.findByTenantIdAndCode(tenantId, "1110").orElseThrow();
        donationIncome = accountRepository.findByTenantIdAndCode(tenantId, "4200").orElseThrow();
        expenseAccount = accountRepository.findByTenantIdAndCode(tenantId, "5210").orElseThrow();

        transactionService.recordJournalEntry(tenantId, LocalDate.now().minusDays(1), "Donation",
                List.of(
                        JournalEntryLine.builder()
                                .accountId(bank.getId())
                                .debit(new BigDecimal("100.00"))
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryLine.builder()
                                .accountId(donationIncome.getId())
                                .debit(BigDecimal.ZERO)
                                .credit(new BigDecimal("100.00"))
                                .build()
                ));

        transactionService.recordJournalEntry(tenantId, LocalDate.now(), "Utility payment",
                List.of(
                        JournalEntryLine.builder()
                                .accountId(expenseAccount.getId())
                                .debit(new BigDecimal("20.00"))
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryLine.builder()
                                .accountId(bank.getId())
                                .debit(BigDecimal.ZERO)
                                .credit(new BigDecimal("20.00"))
                                .build()
                ));
    }

    @Test
    void reconcileStatementIdentifiesOutstandingAndUnrecordedItems() {
        BankStatementLine deposit = new BankStatementLine();
        deposit.setDate(LocalDate.now().minusDays(1));
        deposit.setDescription("Bank deposit");
        deposit.setAmount(new BigDecimal("100.00"));

        BankStatementLine fee = new BankStatementLine();
        fee.setDate(LocalDate.now());
        fee.setDescription("Bank service fee");
        fee.setAmount(new BigDecimal("-5.00"));

        ReconciliationResult result = reconciliationService.reconcileStatement(
                tenantId,
                bank.getId(),
                List.of(deposit, fee),
                new BigDecimal("95.00"));

        assertThat(result.getOutstandingTransactions()).hasSize(1);
        assertThat(result.getUnrecordedBankItems()).hasSize(1);
        assertThat(result.getOutstandingTotal()).isEqualByComparingTo(new BigDecimal("-20.00"));
        assertThat(result.getUnrecordedTotal()).isEqualByComparingTo(new BigDecimal("-5.00"));
        assertThat(result.getAdjustedBankBalance()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.getAdjustedBookBalance()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(result.getDifference()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
