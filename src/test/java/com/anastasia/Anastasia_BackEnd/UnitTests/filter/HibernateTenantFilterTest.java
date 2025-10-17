package com.anastasia.Anastasia_BackEnd.UnitTests.filter;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.filter.HibernateTenantFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HibernateTenantFilterTest {

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @InjectMocks
    private HibernateTenantFilter hibernateTenantFilter;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void doFilter_whenTenantPresent_shouldEnableHibernateFilter() throws ServletException, IOException {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        Session session = mock(Session.class);
        Filter filter = mock(Filter.class);
        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("tenantFilter")).thenReturn(filter);

        FilterChain chain = mock(FilterChain.class);

        hibernateTenantFilter.doFilter(mock(HttpServletRequest.class), mock(HttpServletResponse.class), chain);

        verify(filter).setParameter("tenantId", tenantId);
        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void doFilter_whenTenantMissing_shouldSkipFilterActivation() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);

        hibernateTenantFilter.doFilter(mock(HttpServletRequest.class), mock(HttpServletResponse.class), chain);

        verify(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }
}
