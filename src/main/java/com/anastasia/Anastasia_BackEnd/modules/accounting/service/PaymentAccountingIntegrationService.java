package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.ResourceNotFoundException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Fund;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentAccountingIntegrationService {

    private static final String DEFAULT_ASSET_ACCOUNT_CODE = "1110";
    private static final String DEFAULT_FEE_ACCOUNT_CODE = "5210";

    private static final Map<PaymentPurpose, String> PURPOSE_TO_INCOME_CODE = new EnumMap<>(PaymentPurpose.class);

    static {
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.TITHE, "4100");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.CONTRIBUTION, "4200");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.DONATION, "4200");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.CERTIFICATE, "4300");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.SUBSCRIPTION, "4400");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.STORE_PURCHASE, "4500");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.SUNDAY_SCHOOL_DONATION, "4600");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.SPECIAL_EVENT_PAYMENT, "4700");
        PURPOSE_TO_INCOME_CODE.put(PaymentPurpose.OTHER, "4990");
    }

    private final AccountRepository accountRepository;
    private final FundRepository fundRepository;

    @Transactional(readOnly = true)
    public AccountingMapping resolve(String tenantId, PaymentPurpose purpose, String fundId) {
        Account assetAccount = resolveAccountByCodeOrFallback(tenantId, DEFAULT_ASSET_ACCOUNT_CODE, AccountType.ASSET);
        Account feeAccount = resolveAccountByCodeOrFallback(tenantId, DEFAULT_FEE_ACCOUNT_CODE, AccountType.EXPENSE);
        Account incomeAccount = resolveIncomeAccount(tenantId, purpose);
        Fund fund = resolveFund(tenantId, fundId);

        return new AccountingMapping(assetAccount, incomeAccount, feeAccount, fund);
    }

    private Account resolveIncomeAccount(String tenantId, PaymentPurpose purpose) {
        String code = PURPOSE_TO_INCOME_CODE.getOrDefault(purpose, "4990");
        return resolveAccountByCodeOrFallback(tenantId, code, AccountType.INCOME);
    }

    private Account resolveAccountByCodeOrFallback(String tenantId, String code, AccountType type) {
        Optional<Account> account = accountRepository.findByTenantIdAndCode(tenantId, code);
        if (account.isPresent()) {
            return account.get();
        }

        List<Account> accountsByType = accountRepository.findByTenantIdAndType(tenantId, type);
        return accountsByType.stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No " + type + " account configured for tenant"));
    }

    private Fund resolveFund(String tenantId, String fundId) {
        if (fundId == null || fundId.isBlank()) {
            return null;
        }
        try {
            Long parsedId = Long.valueOf(fundId);
            return fundRepository.findByIdAndTenantId(parsedId, tenantId)
                    .orElse(null);
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse fundId {} for tenant {}", fundId, tenantId, ex);
            return null;
        }
    }

    @Value
    public static class AccountingMapping {
        Account assetAccount;
        Account incomeAccount;
        Account feeAccount;
        Fund fund;
    }
}
