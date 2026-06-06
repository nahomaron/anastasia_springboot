package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionUpgradeRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionUpgradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TenantSubscriptionUpgradeRequestRepository extends JpaRepository<TenantSubscriptionUpgradeRequestEntity, UUID> {

    Optional<TenantSubscriptionUpgradeRequestEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<TenantSubscriptionUpgradeRequestEntity> findByProviderCheckoutSessionId(String providerCheckoutSessionId);

    Optional<TenantSubscriptionUpgradeRequestEntity> findByProviderAndProviderSubscriptionId(
            com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider provider,
            String providerSubscriptionId
    );

    Optional<TenantSubscriptionUpgradeRequestEntity> findFirstByTenantSubscription_IdAndStatusInOrderByCreatedAtDesc(
            UUID tenantSubscriptionId,
            Collection<TenantSubscriptionUpgradeStatus> statuses
    );
}
