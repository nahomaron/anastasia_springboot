package com.anastasia.Anastasia_BackEnd.UnitTests.config;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setTenantId_shouldMakeTenantAvailable() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        assertThat(TenantContext.hasTenantId()).isTrue();
        assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void clear_shouldRemoveTenant() {
        TenantContext.setTenantId(UUID.randomUUID());
        TenantContext.clear();

        assertThat(TenantContext.hasTenantId()).isFalse();
        assertThat(TenantContext.getTenantId()).isNull();
    }
}
