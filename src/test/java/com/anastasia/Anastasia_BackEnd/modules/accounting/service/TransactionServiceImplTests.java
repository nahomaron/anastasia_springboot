package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.JournalEntryLine;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.PaymentCapturedMessage;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransactionDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class TransactionServiceImplTests {

    private static final String MAIN_BANK_CODE = "1110";
    private static final String PROCESSING_FEES_CODE = "5210";
    private static final String DONATION_INCOME_CODE = "4200";

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ChartOfAccountsService chartOfAccountsService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        chartOfAccountsService.createInitialChartOfAccounts(tenantId);
    }

    @Test
    void recordPaymentCapturePostsDoubleEntryAndIsIdempotent() {
        PaymentCapturedMessage message = PaymentCapturedMessage.builder()
                .tenantId(tenantId)
                .paymentId(UUID.randomUUID().toString())
                .providerRef("stripe_ref")
                .purpose("DONATION")
                .currency("USD")
                .grossAmountMinor(10_000L)
                .feeAmountMinor(300L)
                .netAmountMinor(9_700L)
                .capturedAt(Instant.now())
                .build();

        TransactionDto txn = transactionService.recordPaymentCapture(message);

        assertThat(txn.getLedgerEntries()).hasSize(3);

        Account bank = accountRepository.findByTenantIdAndCode(tenantId, MAIN_BANK_CODE).orElseThrow();
        Account fees = accountRepository.findByTenantIdAndCode(tenantId, PROCESSING_FEES_CODE).orElseThrow();
        Account donations = accountRepository.findByTenantIdAndCode(tenantId, DONATION_INCOME_CODE).orElseThrow();

        assertThat(bank.getBalance()).isEqualByComparingTo(new BigDecimal("97.00"));
        assertThat(fees.getBalance()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(donations.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));

        long countAfterFirst = transactionRepository.count();

        TransactionDto replay = transactionService.recordPaymentCapture(message);
        assertThat(replay.getId()).isEqualTo(txn.getId());
        assertThat(transactionRepository.count()).isEqualTo(countAfterFirst);

        Account bankAfterReplay = accountRepository.findByTenantIdAndCode(tenantId, MAIN_BANK_CODE).orElseThrow();
        assertThat(bankAfterReplay.getBalance()).isEqualByComparingTo(new BigDecimal("97.00"));
    }

    @Test
    void recordJournalEntryPersistsBalancedLines() {
        PaymentCapturedMessage baseline = PaymentCapturedMessage.builder()
                .tenantId(tenantId)
                .paymentId(UUID.randomUUID().toString())
                .providerRef("baseline")
                .purpose("DONATION")
                .currency("USD")
                .grossAmountMinor(5_000L)
                .feeAmountMinor(0L)
                .netAmountMinor(5_000L)
                .capturedAt(Instant.now())
                .build();
        transactionService.recordPaymentCapture(baseline);

        Account bank = accountRepository.findByTenantIdAndCode(tenantId, MAIN_BANK_CODE).orElseThrow();
        Account donations = accountRepository.findByTenantIdAndCode(tenantId, DONATION_INCOME_CODE).orElseThrow();

        transactionService.recordJournalEntry(
                tenantId,
                LocalDate.now(),
                "Reclassify donation",
                List.of(
                        JournalEntryLine.builder()
                                .accountId(donations.getId())
                                .debit(new BigDecimal("50.00"))
                                .credit(BigDecimal.ZERO)
                                .build(),
                        JournalEntryLine.builder()
                                .accountId(bank.getId())
                                .debit(BigDecimal.ZERO)
                                .credit(new BigDecimal("50.00"))
                                .build()
                ));

        Account updatedBank = accountRepository.findByTenantIdAndCode(tenantId, MAIN_BANK_CODE).orElseThrow();
        Account updatedDonations = accountRepository.findByTenantIdAndCode(tenantId, DONATION_INCOME_CODE).orElseThrow();

        assertThat(updatedBank.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(updatedDonations.getBalance()).isEqualByComparingTo(new BigDecimal("0.00"));
    }
}
