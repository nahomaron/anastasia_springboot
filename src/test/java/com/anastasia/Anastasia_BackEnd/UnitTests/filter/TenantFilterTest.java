package com.anastasia.Anastasia_BackEnd.UnitTests.filter;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.filter.TenantFilter;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Set;
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
        SecurityContextHolder.clearContext();
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

    @Test
    void doFilter_whenPlatformAdminPathAndTokenRolePresent_shouldBypassTenantContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/platform/admin/summary");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Tenant-ID", UUID.randomUUID().toString());
        when(jwtUtil.extractRoles("token")).thenReturn(List.of("ROLE_PLATFORM_ADMIN"));

        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(TenantContext.getTenantId()).isNull();

        tenantFilter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(TenantContext.hasTenantId()).isFalse();
    }

    @Test
    void doFilter_whenTenantScopedUserRequestsDifferentTenant_shouldReturnForbidden() throws ServletException, IOException {
        UUID principalTenantId = UUID.randomUUID();
        UUID requestedTenantId = UUID.randomUUID();

        UserEntity user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("tenant-user@example.com")
                .password("secret")
                .fullName("Tenant User")
                .build();
        user.setTenant(TenantEntity.builder().id(principalTenantId).build());

        UserPrincipal principal = new UserPrincipal(user, Set.of(Role.builder().roleName("USER").build()));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tenant/users");
        request.addHeader("X-Tenant-ID", requestedTenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantFilter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(MockHttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getErrorMessage()).isEqualTo("Tenant access denied");
    }
}
