package com.anastasia.Anastasia_BackEnd.filter;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import jakarta.servlet.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.spi.FilterDefinition;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HibernateTenantFilter implements Filter {

    @PersistenceContext
    private final EntityManager entityManager;

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        UUID tenantId = TenantContext.getTenantId();

        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
        }

        chain.doFilter(request, response);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public FilterDefinition getFilterDefinition() {
        return null;
    }

    @Override
    public Filter setParameter(String s, Object o) {
        return null;
    }

    @Override
    public Filter setParameterList(String s, Collection<?> collection) {
        return null;
    }

    @Override
    public Filter setParameterList(String s, Object[] objects) {
        return null;
    }

    @Override
    public void validate() throws HibernateException {

    }

    @Override
    public boolean isAutoEnabled() {
        return false;
    }

    @Override
    public boolean isAppliedToLoadByKey() {
        return false;
    }
}
