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

import java.time.Instant;
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
        Instant now = Instant.now();
        List<OutboxEntity> batch = em.createQuery(
                        "select o from OutboxEntity o where o.status in (:pending, :failed) and (o.nextAttemptAt is null or o.nextAttemptAt <= :now) order by o.createdAt asc",
                        OutboxEntity.class)
                .setParameter("pending", OutboxStatus.PENDING)
                .setParameter("failed", OutboxStatus.FAILED)
                .setParameter("now", now)
                .setMaxResults(100)
                .getResultList();

        for (var e : batch) {
            try {
                Instant attemptAt = Instant.now();
                JsonNode payload = e.getPayload();
                PaymentEventType type = PaymentEventType.valueOf(e.getType());
                publisher.publish(type, e.getTenantId(), e.getAggregateId(), payload);
                e.setAttemptCount(e.getAttemptCount() + 1);
                e.setLastAttemptAt(attemptAt);
                e.setPublishedAt(attemptAt);
                e.setFailedAt(null);
                e.setLastError(null);
                e.setNextAttemptAt(null);
                e.setStatus(OutboxStatus.PUBLISHED);
                em.merge(e);
            } catch (Exception ex) {
                Instant failureAt = Instant.now();
                int nextAttemptCount = e.getAttemptCount() + 1;
                e.setAttemptCount(nextAttemptCount);
                e.setLastAttemptAt(failureAt);
                e.setFailedAt(failureAt);
                e.setStatus(OutboxStatus.FAILED);
                e.setLastError(trimError(ex.getMessage()));
                e.setNextAttemptAt(failureAt.plusSeconds(backoffSeconds(nextAttemptCount)));
                em.merge(e);
                log.error("Failed to publish outbox event id={} type={} tenant={}: {}", e.getId(), e.getType(),
                        e.getTenantId(), ex.getMessage());
                // leave as is; will retry next tick
            }
        }
    }

    private long backoffSeconds(int attemptCount) {
        return Math.min(300L, Math.max(30L, attemptCount * 30L));
    }

    private String trimError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 1024 ? error : error.substring(0, 1024);
    }
}
