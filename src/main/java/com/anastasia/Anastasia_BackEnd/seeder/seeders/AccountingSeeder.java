package com.anastasia.Anastasia_BackEnd.seeder.seeders;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordExpenseRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.RecordIncomeRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransferFundsRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.TransactionRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.ChartOfAccountsService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.FundService;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class AccountingSeeder {

    private static final Logger log = LoggerFactory.getLogger(AccountingSeeder.class);

    private final ChartOfAccountsService chartOfAccountsService;
    private final FundService fundService;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TenantRepository tenantRepository;

    private static final List<String> FUND_NAMES = List.of("Building", "Missions", "Benevolence");

    public void seedAccounting(List<TenantEntity> tenants) {
        if (tenants == null || tenants.isEmpty()) {
            tenants = tenantRepository.findAll();
        }
        if (tenants.isEmpty()) {
            log.warn("Skipping accounting seeding; no tenants available.");
            return;
        }

        Faker faker = new Faker();
        int processed = 0;

        for (TenantEntity tenant : tenants) {
            if (processed >= 5) {
                break; // Prevent runaway seeding in large datasets
            }
            String tenantId = Optional.ofNullable(tenant.getId())
                    .map(UUID::toString)
                    .orElse(null);

            if (tenantId == null) {
                log.debug("Skipping tenant without identifier: {}", tenant.getOwnerName());
                continue;
            }

            // Always ensure the base chart exists (idempotent).
            chartOfAccountsService.createInitialChartOfAccounts(tenantId);

            if (!transactionRepository.findByTenantId(tenantId).isEmpty()) {
                log.debug("Skipping accounting seed for tenant {} (transactions already present).", tenantId);
                continue;
            }

            seedFunds(tenantId, faker);
            seedSampleTransactions(tenantId, faker);
            processed++;
        }

        log.info("Accounting seeding completed for {} tenant(s).", processed);
    }

    private void seedFunds(String tenantId, Faker faker) {
        for (String fundName : FUND_NAMES) {
            CreateFundRequest request = new CreateFundRequest();
            request.setTenantId(tenantId);
            request.setName(fundName + " Fund");
            request.setDescription(faker.lorem().sentence());
            request.setGoalAmount(BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(5_000, 30_000)));

            try {
                fundService.createFund(request);
            } catch (Exception ex) {
                log.debug("Fund {} could not be created for tenant {}: {}", fundName, tenantId, ex.getMessage());
            }
        }
    }

    private void seedSampleTransactions(String tenantId, Faker faker) {
        Account bankAccount = requireAccount(tenantId, "1110");
        Account cashAccount = requireAccount(tenantId, "1120");
        Account titheIncome = requireAccount(tenantId, "4100");
        Account donationIncome = requireAccount(tenantId, "4200");
        Account salaryExpense = requireAccount(tenantId, "5100");
        Account churchExpense = requireAccount(tenantId, "5300");

        // Income: Weekly tithe
        recordIncome(tenantId, bankAccount.getId(), titheIncome.getId(),
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1_000, 5_000)),
                "Weekly Tithe Offering");

        // Income: Special donation
        recordIncome(tenantId, bankAccount.getId(), donationIncome.getId(),
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(2_000, 6_000)),
                "Special Mission Donation");

        // Expense: Staff salary payout
        recordExpense(tenantId, bankAccount.getId(), salaryExpense.getId(),
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(1_500, 4_000)),
                "Monthly Staff Salaries");

        // Expense: Church maintenance
        recordExpense(tenantId, cashAccount.getId(), churchExpense.getId(),
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(500, 1_500)),
                "Church Maintenance Supplies");

        // Transfer: Move cash deposits into the bank
        transferFunds(tenantId, cashAccount.getId(), bankAccount.getId(),
                BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(800, 2_500)),
                "Deposit cash offerings to bank");
    }

    private void recordIncome(String tenantId,
                              Long assetAccountId,
                              Long incomeAccountId,
                              BigDecimal amount,
                              String description) {
        RecordIncomeRequest request = new RecordIncomeRequest();
        request.setTenantId(tenantId);
        request.setDate(randomRecentDate());
        request.setDescription(description);
        request.setAmount(amount);
        request.setAssetAccountId(assetAccountId);
        request.setIncomeAccountId(incomeAccountId);

        try {
            transactionService.recordIncome(request);
        } catch (Exception ex) {
            log.debug("Failed to record income for tenant {}: {}", tenantId, ex.getMessage());
        }
    }

    private void recordExpense(String tenantId,
                               Long assetAccountId,
                               Long expenseAccountId,
                               BigDecimal amount,
                               String description) {
        RecordExpenseRequest request = new RecordExpenseRequest();
        request.setTenantId(tenantId);
        request.setDate(randomRecentDate());
        request.setDescription(description);
        request.setAmount(amount);
        request.setAssetAccountId(assetAccountId);
        request.setExpenseAccountId(expenseAccountId);

        try {
            transactionService.recordExpense(request);
        } catch (Exception ex) {
            log.debug("Failed to record expense for tenant {}: {}", tenantId, ex.getMessage());
        }
    }

    private void transferFunds(String tenantId,
                               Long fromAccountId,
                               Long toAccountId,
                               BigDecimal amount,
                               String description) {
        TransferFundsRequest request = new TransferFundsRequest();
        request.setTenantId(tenantId);
        request.setDate(randomRecentDate());
        request.setDescription(description);
        request.setAmount(amount);
        request.setFromAssetAccountId(fromAccountId);
        request.setToAssetAccountId(toAccountId);

        try {
            transactionService.transferFunds(request);
        } catch (Exception ex) {
            log.debug("Failed to transfer funds for tenant {}: {}", tenantId, ex.getMessage());
        }
    }

    private Account requireAccount(String tenantId, String code) {
        return accountRepository.findByTenantIdAndCode(tenantId, code)
                .orElseThrow(() -> new IllegalStateException(
                        "Required account with code " + code + " not found for tenant " + tenantId));
    }

    private LocalDate randomRecentDate() {
        return LocalDate.now().minusDays(ThreadLocalRandom.current().nextInt(1, 30));
    }
}
