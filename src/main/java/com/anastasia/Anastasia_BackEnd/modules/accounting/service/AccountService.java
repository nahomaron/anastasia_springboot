package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.AccountDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateAccountRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;

import java.util.List;

public interface AccountService {
    AccountDto createAccount(CreateAccountRequest request);
    AccountDto getAccountById(Long id, String tenantId);
    List<AccountDto> getAccountsByTenantId(String tenantId);
    List<AccountDto> getAccountsByTenantIdAndType(String tenantId, AccountType type);
    AccountDto updateAccount(Long id, String tenantId, CreateAccountRequest request);
    void deleteAccount(Long id, String tenantId);
}
