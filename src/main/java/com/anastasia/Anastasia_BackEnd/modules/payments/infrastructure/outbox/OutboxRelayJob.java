package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.kafka.publisher.PaymentEventPublisher;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayJob {
    @PersistenceContext private EntityManager em;
    private final PaymentEventPublisher publisher;
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
                JsonNode payload = mapper.readTree(e.getPayload());
                publisher.publish(e.getType(), e.getTenantId(), e.getAggregateId(), payload);
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
