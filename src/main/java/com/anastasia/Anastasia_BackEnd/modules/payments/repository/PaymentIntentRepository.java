package com.anastasia.Anastasia_BackEnd.modules.payments.repository;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    Optional<PaymentIntent> findByTenantIdAndIdempotencyKey(UUID tenantId, String key);

    // PaymentIntentRepository
    Page<PaymentIntent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Optional<PaymentIntent> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<PaymentIntent> findTopByTenantIdAndStatusOrderByCapturedAtDesc(UUID tenantId,
                                                                            com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus status);

    @Query("""
    SELECT COALESCE(SUM(COALESCE(p.capturedGrossAmountMinor, p.amount.amount)), 0)
    FROM PaymentIntent p
    WHERE p.tenantId = :tenantId
      AND p.status = com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus.CAPTURED
      AND p.capturedAt >= :start
      AND p.capturedAt < :end
    """)
    long sumCapturedAmountByTenantAndCapturedAtBetween(UUID tenantId, java.time.Instant start, java.time.Instant end);

    @Query("""
    SELECT new map(p.fundId as fundId, SUM(p.amount.amount) as total)
    FROM PaymentIntent p
    WHERE p.tenantId = :tenantId AND p.status = com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentStatus.CAPTURED
    GROUP BY p.fundId
""")
    List<Map<String, Object>> totalCapturedByFund(UUID tenantId);
}
