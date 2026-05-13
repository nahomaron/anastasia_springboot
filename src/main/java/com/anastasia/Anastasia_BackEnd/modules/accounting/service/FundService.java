package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.FundDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.UpdateFundRequest;

import java.util.List;
import java.util.UUID;

public interface FundService {
    FundDto createFund(CreateFundRequest request);
    FundDto getFundById(Long id, UUID tenantId);
    List<FundDto> getFundsByTenantId(UUID tenantId);
    FundDto updateFund(Long id, UpdateFundRequest request);
    void deleteFund(Long id, UUID tenantId);
}
