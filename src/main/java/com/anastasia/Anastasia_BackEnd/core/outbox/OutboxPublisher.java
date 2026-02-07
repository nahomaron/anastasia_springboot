package com.anastasia.Anastasia_BackEnd.core.outbox;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Generic OutboxPublisher that persists domain events transactionally
 * into the outbox_events table. Supports any event type (Payment, Registration, etc.)
 */
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    @PersistenceContext
    private final EntityManager em;
    private final ObjectMapper mapper;

    /**
     * Publishes a domain event into the outbox table.
     *
     * @param eventType The enum describing the event type
     * @param tenantId  The tenant context (optional)
     * @param aggregateId The ID of the entity that triggered the event (optional)
     * @param payload   The payload object (will be serialized to JSON)
     */

    public void publish(Enum<?> eventType, Object payload) {
        publish(eventType, TenantContext.getTenantId(), null, payload);
    }

    @Transactional
    public void publish(Enum<?> eventType,
                        UUID tenantId,
                        String aggregateId,
                        Object payload) {

        var entity = new OutboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType(eventType.getClass().getSimpleName());
        entity.setAggregateId(aggregateId);
        entity.setTenantId(tenantId);
        entity.setType(eventType.name());
        entity.setPayload(toJsonNode(payload));
        entity.setHeaders(toJsonNode(buildHeaders(eventType, tenantId)));
        entity.setCreatedAt(Instant.now());
        entity.setPublished(false);

        em.persist(entity);
    }



    private Map<String, Object> buildHeaders(Enum<?> eventType, UUID tenantId) {
        Map<String, Object> headers = new HashMap<>();
        if (tenantId != null) headers.put("tenantId", tenantId.toString());
        headers.put("eventType", eventType.name());
        headers.put("source", "OutboxPublisher");
        return headers;
    }

    private JsonNode toJsonNode(Object data) {
        try {
            return mapper.valueToTree(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}
