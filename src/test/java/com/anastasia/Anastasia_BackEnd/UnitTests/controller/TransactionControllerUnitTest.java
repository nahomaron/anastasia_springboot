package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.TransactionController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.TransactionDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.enums.TransactionType;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TransactionControllerUnitTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    @Test
    void getTransactionByIdReturnsTransactionPayload() {
        UUID tenantId = UUID.randomUUID();
        TransactionDto expected = TransactionDto.builder()
                .id(42L)
                .description("Sunday offering")
                .type(TransactionType.INCOME)
                .build();
        when(transactionService.getTransactionById(42L, tenantId)).thenReturn(expected);

        var response = transactionController.getTransactionById(42L, tenantId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getTransactionsPassesThroughFilters() {
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);
        List<TransactionDto> expected = List.of(
                TransactionDto.builder().id(10L).type(TransactionType.EXPENSE).build()
        );
        when(transactionService.getTransactions(tenantId, startDate, endDate, 7L)).thenReturn(expected);

        var response = transactionController.getTransactions(tenantId, startDate, endDate, 7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(transactionService).getTransactions(tenantId, startDate, endDate, 7L);
    }
}
