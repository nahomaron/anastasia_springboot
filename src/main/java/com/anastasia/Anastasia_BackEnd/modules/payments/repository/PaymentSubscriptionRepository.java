package com.anastasia.Anastasia_BackEnd.modules.payments.repository;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentSubscriptionRepository extends JpaRepository<PaymentSubscription, UUID> {
    List<PaymentSubscription> findByTenantId(UUID tenantId);
    Optional<PaymentSubscription> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    // PaymentSubscriptionRepository
    Page<PaymentSubscription> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Optional<PaymentSubscription> findByIdAndTenantId(UUID id, UUID tenantId);

}
