package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {
    Optional<PaymentIntent> findByIdempotencyKey(String key);
}
