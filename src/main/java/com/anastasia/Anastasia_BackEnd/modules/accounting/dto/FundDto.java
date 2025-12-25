package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FundDto {
    private Long id;
    private UUID tenantId;
    private String name;
    private String description;
    private BigDecimal goalAmount;
    private BigDecimal currentBalance;
    private Long associatedEquityAccountId;
}
