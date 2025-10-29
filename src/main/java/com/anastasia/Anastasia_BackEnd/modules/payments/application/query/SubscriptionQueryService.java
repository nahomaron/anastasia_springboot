package com.anastasia.Anastasia_BackEnd.modules.payments.application.query;

import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.repository.PaymentSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.SubscriptionView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryService {

    private final PaymentSubscriptionRepository repo;

    public Page<SubscriptionView> findAll(UUID tenantId, Pageable pageable) {
        return repo.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(SubscriptionView::fromEntity);
    }

    public SubscriptionView findById(UUID tenantId, UUID id) {
        var sub = repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
        return SubscriptionView.fromEntity(sub);
    }
}
