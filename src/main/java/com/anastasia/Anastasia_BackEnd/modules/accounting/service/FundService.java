package com.anastasia.Anastasia_BackEnd.modules.accounting.service;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.CreateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.FundDto;

import java.util.List;

public interface FundService {
    FundDto createFund(CreateFundRequest request);
    FundDto getFundById(Long id, String tenantId);
    List<FundDto> getFundsByTenantId(String tenantId);
}
