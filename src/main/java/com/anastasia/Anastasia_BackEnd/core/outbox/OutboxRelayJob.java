package com.anastasia.Anastasia_BackEnd.core.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.anastasia.Anastasia_BackEnd.core.kafka.publisher.DomainEventPublisher;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A scheduled job that relays unpublished outbox events to the Kafka publisher.
 * It runs every second and processes events in batches.
 * It’s like a postman that looks into the “Outbox mailbox” every second and
 * sends any unsent messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = false)
public class OutboxRelayJob {
    @PersistenceContext private EntityManager em;
    private final DomainEventPublisher publisher;
    private final ObjectMapper mapper;

    @Scheduled(fixedDelay = 1000L)
    @Transactional
    public void relay() {
        List<OutboxEntity> batch = em.createQuery(
                        "select o from OutboxEntity o where o.published=false order by o.createdAt asc", OutboxEntity.class)
                .setMaxResults(100)
                .getResultList();

        for (var e : batch) {
            try {
                JsonNode payload = e.getPayload();
                PaymentEventType type = PaymentEventType.valueOf(e.getType());
                publisher.publish(type, e.getTenantId(), e.getAggregateId(), payload);
                e.setPublished(true);
                em.merge(e);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event id={} type={} tenant={}: {}", e.getId(), e.getType(),
                        e.getTenantId(), ex.getMessage());
                // leave as is; will retry next tick
            }
        }
    }
}
