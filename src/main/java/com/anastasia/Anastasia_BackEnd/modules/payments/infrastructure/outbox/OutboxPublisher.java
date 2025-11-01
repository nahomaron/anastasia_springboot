package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    @PersistenceContext private EntityManager em;
    private final ObjectMapper mapper;

    @Transactional
    public void publish(PaymentEventType type, String aggregateId, UUID tenantId, Map<String,Object> payload) {
        var entity = new OutboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType(type.getAggregateType());
        entity.setAggregateId(aggregateId);
        entity.setTenantId(tenantId);
        entity.setType(type.name());
        entity.setPayload(mapper.valueToTree(payload).toString());
        Map<String, Object> headers = new java.util.HashMap<>();
        if (tenantId != null) {
            headers.put("tenantId", tenantId.toString());
        }
        headers.put("eventType", type.name());
        entity.setHeaders(mapper.valueToTree(headers).toString());
        entity.setCreatedAt(Instant.now());
        entity.setPublished(false);
        em.persist(entity);
    }

}
