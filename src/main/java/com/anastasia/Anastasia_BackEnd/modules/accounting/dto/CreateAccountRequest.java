package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;


import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAccountRequest {
    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Account name is required")
    private String name;

    @NotBlank(message = "Account code is required")
    private String code;

    @NotNull(message = "Account type is required")
    private AccountType type;

    private String description;
    private Long parentAccountId;
}
