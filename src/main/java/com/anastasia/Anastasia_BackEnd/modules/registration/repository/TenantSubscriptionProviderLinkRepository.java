package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionProviderLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantSubscriptionProviderLinkRepository extends JpaRepository<TenantSubscriptionProviderLinkEntity, UUID> {

    Optional<TenantSubscriptionProviderLinkEntity> findByProviderAndProviderSubscriptionId(
            BillingProvider provider,
            String providerSubscriptionId
    );

    Optional<TenantSubscriptionProviderLinkEntity> findByTenantSubscription_IdAndProvider(
            UUID tenantSubscriptionId,
            BillingProvider provider
    );
}
