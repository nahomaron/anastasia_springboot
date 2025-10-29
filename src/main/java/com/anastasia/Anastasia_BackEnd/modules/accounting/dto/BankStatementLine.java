package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BankStatementLine {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
}
