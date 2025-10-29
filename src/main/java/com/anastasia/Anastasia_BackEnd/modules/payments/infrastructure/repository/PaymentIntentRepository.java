package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    Optional<PaymentIntent> findByTenantIdAndIdempotencyKey(UUID tenantId, String key);

    // PaymentIntentRepository
    Page<PaymentIntent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Optional<PaymentIntent> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
    SELECT new map(p.fundId as fundId, SUM(p.amount.amount) as total)
    FROM PaymentIntent p
    WHERE p.tenantId = :tenantId AND p.status = com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus.CAPTURED
    GROUP BY p.fundId
""")
    List<Map<String, Object>> totalCapturedByFund(UUID tenantId);
}
