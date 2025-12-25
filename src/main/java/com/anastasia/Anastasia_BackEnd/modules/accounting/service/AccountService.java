package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.AccountDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateAccountRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;

import java.util.List;
import java.util.UUID;

public interface AccountService {
    AccountDto createAccount(CreateAccountRequest request);
    AccountDto getAccountById(Long id, UUID tenantId);
    List<AccountDto> getAccountsByTenantId(UUID tenantId);
    List<AccountDto> getAccountsByTenantIdAndType(UUID tenantId, AccountType type);
    AccountDto updateAccount(Long id, UUID tenantId, CreateAccountRequest request);
    void deleteAccount(Long id, UUID tenantId);
}
