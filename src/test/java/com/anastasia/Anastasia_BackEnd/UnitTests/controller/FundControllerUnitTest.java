package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.controller.FundController;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.FundDto;
import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.UpdateFundRequest;
import com.anastasia.Anastasia_BackEnd.modules.accounting.security.AccountingTenantResolver;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.FundService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class FundControllerUnitTest {

    @Mock
    private FundService fundService;

    @Mock
    private AccountingTenantResolver tenantResolver;

    @InjectMocks
    private FundController fundController;

    @Test
    void updateFundReturnsUpdatedPayload() {
        UUID tenantId = UUID.randomUUID();
        UpdateFundRequest request = new UpdateFundRequest();
        request.setTenantId(tenantId);
        request.setName("Building Fund");

        FundDto updated = FundDto.builder()
                .id(5L)
                .name("Building Fund")
                .build();
        when(tenantResolver.resolveTenant(tenantId)).thenReturn(tenantId);
        when(fundService.updateFund(5L, request)).thenReturn(updated);

        var response = fundController.updateFund(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(updated);
        verify(tenantResolver).resolveTenant(tenantId);
    }

    @Test
    void deleteFundReturnsNoContent() {
        UUID tenantId = UUID.randomUUID();
        when(tenantResolver.resolveTenant(tenantId)).thenReturn(tenantId);

        var response = fundController.deleteFund(9L, tenantId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(fundService).deleteFund(9L, tenantId);
    }
}
