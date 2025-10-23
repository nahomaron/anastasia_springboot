package com.anastasia.Anastasia_BackEnd.config;


import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.service.registration.MemberService;
import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class CacheWarmupConfig {

    private final TenantRepository tenantRepository;
    private final MemberService memberService;

    public CacheWarmupConfig(TenantRepository tenantRepository,
                             MemberService memberService) {
        this.tenantRepository = tenantRepository;
        this.memberService = memberService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preloadCache() {
        System.out.println("Preloading member caches...");
        tenantRepository.findAll().forEach(tenant -> {
            TenantContext.setTenantId(tenant.getId());
            memberService.findAll(PageRequest.of(0, 10)); // first page per tenant
        });
        TenantContext.clear(); // reset context
    }
}
