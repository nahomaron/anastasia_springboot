package com.anastasia.Anastasia_BackEnd.config;


import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Custom cache key generator that ensures:
 *  - Tenant isolation (prefix every key with tenantId)
 *  - Unique keys for Pageable and other params
 */
@Component("tenantAwareKeyGenerator")
public class TenantAwareCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        // Always start with tenantId prefix
        String tenantId = String.valueOf(TenantContext.getTenantId());
        StringBuilder key = new StringBuilder("tenant:")
                .append(tenantId != null ? tenantId : "unknown");

        // Append method name for extra isolation (optional but helps debugging)
        key.append(":").append(method.getName());

        // Append all method parameters
        for (Object param : params) {
            if (param == null) continue;

            // Handle pagination
            if (param instanceof Pageable pageable) {
                key.append(":page=").append(pageable.getPageNumber())
                        .append(":size=").append(pageable.getPageSize())
                        .append(":sort=").append(pageable.getSort());
            } else {
                key.append(":").append(param.toString());
            }
        }

        return key.toString();
    }
}
