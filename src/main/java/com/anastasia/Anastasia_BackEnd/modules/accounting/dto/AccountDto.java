package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountDto {
    private Long id;
    private String name;
    private String code;
    private AccountType type;
    private String description;
    private BigDecimal balance;
    private Long parentAccountId;
}
