package com.anastasia.Anastasia_BackEnd.common.cache;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final TenantRepository tenantRepository;
    private final MemberService memberService;
    private final CacheManager cacheManager;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;
    private static final String CACHE_KEY_PREFIX = "cache:warmup:tenant:";

    /**
     * Warm up cache for a specific tenant
     */
    @Async("cacheExecutor")
    public void warmUpTenantCache(UUID tenantId) {
        String redisKey = CACHE_KEY_PREFIX + tenantId;
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();

        // check if recently warmed (within 30 minutes)
        Instant lastWarmed = redisTemplate == null
                ? null
                : parseLastWarmed(redisTemplate.opsForValue().get(redisKey));
        if (lastWarmed != null && lastWarmed.isAfter(Instant.now().minus(Duration.ofMinutes(30)))) {
            log.info("⚡ Cache for tenant {} already warmed at {}. Skipping.", tenantId, lastWarmed);
            return;
        }

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid tenant id: " + tenantId));

        TenantContext.setTenantId(tenant.getId());
        log.info("🔄 Starting cache warm-up for tenant: {} ({})", tenant.getOwnerName(), tenant.getId());

        try {
            var page = memberService.findAll(PageRequest.of(0, 10));
            log.info("✅ Cached {} members for tenant {}", page.getContent().size(), tenant.getOwnerName());

            // record warm-up timestamp in Redis (expires after 30 minutes)
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(redisKey, Instant.now().toEpochMilli(), Duration.ofMinutes(30));
            }

        } catch (Exception e) {
            log.error("Error warming cache for tenant {}: {}", tenant.getOwnerName(), e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
//        // Preload first few pages or summary data
//        Page<?> membersPage = memberService.findAll(PageRequest.of(0, 10));
//
//        int count = membersPage.getContent().size();
//        log.info("✅ Cached {} members for tenant {}", count, tenant.getOwnerName());
//
//        // Add more preloads here if needed (roles, permissions, etc.)
//        TenantContext.clear();
    }

    /**
     * Optional — clear all caches for this tenant
     */
    public void clearTenantCaches(UUID tenantId) {
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            log.warn("RedisTemplate unavailable, skipping tenant cache clear for {}", tenantId);
            return;
        }

        log.warn("🧹 Clearing caches for tenant {}", tenantId);
        cacheManager.getCacheNames().forEach(name -> clearTenantEntries(redisTemplate, name, tenantId));
    }

    private void clearTenantEntries(RedisTemplate<String, Object> redisTemplate, String cacheName, UUID tenantId) {
        String keyPattern = cacheName + "::tenant:" + tenantId + "*";
        var keys = redisTemplate.keys(keyPattern);
        if (keys == null || keys.isEmpty()) {
            return;
        }

        redisTemplate.delete(keys);
        log.info("Cleared {} entries from cache '{}'", keys.size(), cacheName);
    }

    private Instant parseLastWarmed(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Instant) {
            return (Instant) rawValue;
        }
        if (rawValue instanceof Number) {
            return Instant.ofEpochMilli(((Number) rawValue).longValue());
        }
        return null;
    }
}
