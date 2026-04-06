package com.anastasia.Anastasia_BackEnd.UnitTests.filter;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.filter.TenantFilter;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class TenantFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private TenantFilter tenantFilter;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void doFilter_whenTenantHeaderPresent_shouldSetTenantContext() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", tenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);

        tenantFilter.doFilter(request, response, chain);

        assertThat(TenantContext.hasTenantId()).isFalse();
    }

    @Test
    void doFilter_whenHeaderMissing_shouldFallbackToToken() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        when(jwtUtil.extractTenantId("token")).thenReturn(tenantId.toString());

        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);

        tenantFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(TenantContext.hasTenantId()).isFalse();
    }

    @Test
    void doFilter_whenTenantIdInvalid_shouldThrowServletException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "not-a-uuid");

        assertThrows(ServletException.class, () -> tenantFilter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class)));
    }

    @Test
    void doFilter_whenJwtExtractionFails_shouldPropagateAsServletException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        doThrow(new RuntimeException("bad token")).when(jwtUtil).extractTenantId("token");

        assertThrows(ServletException.class, () -> tenantFilter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class)));
    }

    @Test
    void doFilter_whenNoTenantAssigned_shouldClearContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        FilterChain chain = mock(FilterChain.class);

        tenantFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(TenantContext.getTenantId()).isNull();
    }
}
