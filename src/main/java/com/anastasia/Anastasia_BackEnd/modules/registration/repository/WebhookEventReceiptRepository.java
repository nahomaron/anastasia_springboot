package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookEventReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEventReceiptRepository extends JpaRepository<WebhookEventReceiptEntity, UUID> {
    List<WebhookEventReceiptEntity> findByTenantId(UUID tenantId);
    Optional<WebhookEventReceiptEntity> findByProviderAndEventId(String provider, String eventId);
}
