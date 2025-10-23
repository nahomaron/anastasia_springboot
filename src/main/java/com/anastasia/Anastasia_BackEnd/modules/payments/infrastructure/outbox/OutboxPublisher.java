package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox;

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
    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public void publish(String type, String aggregateId, String tenantId, Map<String,Object> payload) {
        var entity = new OutboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType("Payment");
        entity.setAggregateId(aggregateId);
        entity.setTenantId(tenantId);
        entity.setType(type);
        entity.setPayload(mapper.valueToTree(payload).toString());
        entity.setHeaders(mapper.valueToTree(Map.of("tenantId", tenantId)).toString());
        entity.setCreatedAt(Instant.now());
        entity.setPublished(false);
        em.persist(entity);
    }
}
