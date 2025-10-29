package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;


import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class LedgerEntryDto {
    private Long id;
    private Long accountId;
    private String accountName;
    private Long fundId;
    private String fundName;
    private BigDecimal debit;
    private BigDecimal credit;
}
