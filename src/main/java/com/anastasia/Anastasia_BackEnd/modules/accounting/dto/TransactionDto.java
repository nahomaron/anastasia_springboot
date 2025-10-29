package com.anastasia.Anastasia_BackEnd.modules.accounting.dto;

import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.TransactionType;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TransactionDto {
    private Long id;
    private LocalDate date;
    private String description;
    private TransactionType type;
    private String externalReference;
    private String sourceSystem;
    private List<LedgerEntryDto> ledgerEntries;
}
