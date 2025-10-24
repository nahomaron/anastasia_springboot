package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentSubscriptionRepository extends JpaRepository<PaymentSubscription, UUID> {
    Optional<PaymentSubscription> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}
