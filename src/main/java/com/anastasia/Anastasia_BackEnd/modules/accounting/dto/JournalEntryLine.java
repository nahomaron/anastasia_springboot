package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JournalEntryLine {
    Long accountId;
    BigDecimal debit;
    BigDecimal credit;
    Long fundId;
}
