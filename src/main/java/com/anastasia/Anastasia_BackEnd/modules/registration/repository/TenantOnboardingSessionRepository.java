package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantOnboardingSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantOnboardingSessionRepository extends JpaRepository<TenantOnboardingSessionEntity, UUID> {
    Optional<TenantOnboardingSessionEntity> findByIdempotencyKey(String idempotencyKey);
    Optional<TenantOnboardingSessionEntity> findByProviderCheckoutSessionId(String providerCheckoutSessionId);
    Optional<TenantOnboardingSessionEntity> findByProviderSubscriptionId(String providerSubscriptionId);
}
