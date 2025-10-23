package com.anastasia.Anastasia_BackEnd.service.cache;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.service.registration.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final TenantRepository tenantRepository;
    private final MemberService memberService;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Object LOCK = new Object();
    private static final Set<UUID> WARMED_TENANTS = new HashSet<>();
    private static final String CACHE_KEY_PREFIX = "cache:warmup:tenant:";

    /**
     * Warm up cache for a specific tenant
     */
    @Async("cacheExecutor")
    public void warmUpTenantCache(UUID tenantId) {
        synchronized (LOCK) {
            // prevent duplicate warm-up if already done recently
            if (WARMED_TENANTS.contains(tenantId)) {
                log.info("⚡ Cache for tenant {} already warmed recently. Skipping.", tenantId);
                return;
            }
            WARMED_TENANTS.add(tenantId);
        }

        String redisKey = CACHE_KEY_PREFIX + tenantId;

        // check if recently warmed (within 30 minutes)
        Instant lastWarmed = (Instant) redisTemplate.opsForValue().get(redisKey);
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
            redisTemplate.opsForValue().set(redisKey, Instant.now(), Duration.ofMinutes(30));

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
        TenantContext.setTenantId(tenantId);
        log.warn("🧹 Clearing all caches for tenant {}", tenantId);
        cacheManager.getCacheNames().forEach(name -> {
            cacheManager.getCache(name).clear();
            log.info("Cleared cache '{}'", name);
        });
        TenantContext.clear();
    }
}
