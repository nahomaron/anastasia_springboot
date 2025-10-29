package com.anastasia.Anastasia_BackEnd.modules.accounting.service.impl;


import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.AccountDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateAccountRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.InvalidTransactionException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.exception.ResourceNotFoundException;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountDto createAccount(CreateAccountRequest request) {
        Account parent = null;
        if (request.getParentAccountId() != null) {
            parent = accountRepository.findByIdAndTenantId(request.getParentAccountId(), request.getTenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent account not found"));
        }

        Account account = Account.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .code(request.getCode())
                .type(request.getType())
                .description(request.getDescription())
                .balance(BigDecimal.ZERO)
                .parentAccount(parent)
                .build();

        return toDto(accountRepository.save(account));
    }

    @Override
    public AccountDto getAccountById(Long id, String tenantId) {
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        return toDto(account);
    }

    @Override
    public List<AccountDto> getAccountsByTenantId(String tenantId) {
        return accountRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountDto> getAccountsByTenantIdAndType(String tenantId, AccountType type) {
        return accountRepository.findByTenantIdAndType(tenantId, type).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public AccountDto updateAccount(Long id, String tenantId, CreateAccountRequest request) {
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        Account parent = null;
        if (request.getParentAccountId() != null) {
            parent = accountRepository.findByIdAndTenantId(request.getParentAccountId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent account not found"));
        }

        account.setName(request.getName());
        account.setCode(request.getCode());
        account.setType(request.getType());
        account.setDescription(request.getDescription());
        account.setParentAccount(parent);

        return toDto(accountRepository.save(account));
    }

    @Override
    public void deleteAccount(Long id, String tenantId) {
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Add business logic here - e.g., cannot delete an account with a non-zero balance
        // or transactions.
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidTransactionException("Cannot delete account: balance is not zero.");
        }
        // You should also check if it has ledger entries.

        accountRepository.delete(account);
    }

    private AccountDto toDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .name(account.getName())
                .code(account.getCode())
                .type(account.getType())
                .description(account.getDescription())
                .balance(account.getBalance())
                .parentAccountId(account.getParentAccount() != null ? account.getParentAccount().getId() : null)
                .build();
    }
}
