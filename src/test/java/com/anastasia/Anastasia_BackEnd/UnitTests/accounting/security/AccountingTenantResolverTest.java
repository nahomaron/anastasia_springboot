package com.anastasia.Anastasia_BackEnd.UnitTests.accounting.security;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.accounting.security.AccountingTenantResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountingTenantResolverTest {

    private final AccountingTenantResolver tenantResolver = new AccountingTenantResolver();

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void resolveTenantUsesCurrentTenantWhenRequestOmitsTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        assertThat(tenantResolver.resolveTenant(null)).isEqualTo(tenantId);
    }

    @Test
    void resolveTenantAllowsMatchingLegacyClientTenantId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        assertThat(tenantResolver.resolveTenant(tenantId)).isEqualTo(tenantId);
    }

    @Test
    void resolveTenantRejectsCrossTenantClientTenantId() {
        TenantContext.setTenantId(UUID.randomUUID());

        assertThatThrownBy(() -> tenantResolver.resolveTenant(UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void resolveTenantRejectsRequestsWithoutTenantContext() {
        assertThatThrownBy(() -> tenantResolver.resolveTenant(null))
                .isInstanceOf(AccessDeniedException.class);
    }
}
