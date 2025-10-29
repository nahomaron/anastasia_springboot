package com.anastasia.Anastasia_BackEnd.modules.accounting.service.impl;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.FundDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.InvalidTransactionException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.ResourceNotFoundException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Fund;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundServiceImpl implements FundService {

    private static final String FUND_PARENT_CODE = "3200";
    private static final String EQUITY_ROOT_CODE = "3000";

    private final FundRepository fundRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public FundDto createFund(CreateFundRequest request) {
        String tenantId = request.getTenantId();

        if (fundRepository.existsByTenantIdAndNameIgnoreCase(tenantId, request.getName())) {
            throw new InvalidTransactionException("Fund with the same name already exists for tenant.");
        }

        Account parent = accountRepository.findByTenantIdAndCode(tenantId, FUND_PARENT_CODE)
                .orElseGet(() -> accountRepository.findByTenantIdAndCode(tenantId, EQUITY_ROOT_CODE)
                        .orElseThrow(() -> new ResourceNotFoundException("Base Equity account not found for tenant")));

        Account equityAccount = Account.builder()
                .tenantId(tenantId)
                .name(request.getName() + " Fund")
                .code(generateFundAccountCode(tenantId))
                .type(AccountType.EQUITY)
                .description(request.getDescription())
                .balance(BigDecimal.ZERO)
                .parentAccount(parent)
                .build();

        Account persistedAccount = accountRepository.save(equityAccount);

        Fund fund = Fund.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .description(request.getDescription())
                .goalAmount(request.getGoalAmount())
                .associatedEquityAccount(persistedAccount)
                .build();

        return toDto(fundRepository.save(fund));
    }

    @Override
    @Transactional(readOnly = true)
    public FundDto getFundById(Long id, String tenantId) {
        Fund fund = fundRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + id));
        return toDto(fund);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundDto> getFundsByTenantId(String tenantId) {
        return fundRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private String generateFundAccountCode(String tenantId) {
        List<Account> tenantEquity = accountRepository.findByTenantIdAndType(tenantId, AccountType.EQUITY);

        int nextSequence = tenantEquity.stream()
                .map(Account::getCode)
                .filter(code -> code != null && code.startsWith("32"))
                .map(code -> code.replaceAll("[^0-9]", ""))
                .filter(s -> !s.isBlank())
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(3200);

        return String.format("%04d", Math.max(3201, nextSequence + 1));
    }

    private FundDto toDto(Fund fund) {
        Account equityAccount = fund.getAssociatedEquityAccount();

        return FundDto.builder()
                .id(fund.getId())
                .tenantId(fund.getTenantId())
                .name(fund.getName())
                .description(fund.getDescription())
                .goalAmount(fund.getGoalAmount())
                .currentBalance(equityAccount != null ? equityAccount.getBalance() : BigDecimal.ZERO)
                .associatedEquityAccountId(equityAccount != null ? equityAccount.getId() : null)
                .build();
    }
}
