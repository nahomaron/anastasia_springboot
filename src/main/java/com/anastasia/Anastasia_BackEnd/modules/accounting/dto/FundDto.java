package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FundDto {
    private Long id;
    private String tenantId;
    private String name;
    private String description;
    private BigDecimal goalAmount;
    private BigDecimal currentBalance;
    private Long associatedEquityAccountId;
}
