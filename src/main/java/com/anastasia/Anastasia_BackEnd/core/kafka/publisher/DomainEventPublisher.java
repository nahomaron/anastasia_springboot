package com.anastasia.Anastasia_BackEnd.core.kafka.publisher;

import com.anastasia.Anastasia_BackEnd.core.kafka.support.KafkaTopicNameResolver;
import com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaHeaderNames;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.events.PaymentEventType;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicNameResolver topicNameResolver;

    public void publish(Enum<?> eventType, UUID tenantId, String key, JsonNode payload) {
        String topic = topicNameResolver.resolve(eventType);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload.toString());
        byte[] tenantBytes = tenantId != null ? tenantId.toString().getBytes(StandardCharsets.UTF_8) : new byte[0];
        record.headers().add(KafkaHeaderNames.TENANT_ID, tenantBytes);
        record.headers().add(KafkaHeaderNames.EVENT_TYPE, eventType.name().getBytes(StandardCharsets.UTF_8));

        kafkaTemplate.send(record).whenComplete((SendResult<String, Object> result, Throwable ex) -> {
            if (ex != null) {
                log.error("Failed to publish payment event type={} key={}: {}", eventType, key, ex.getMessage());
            } else if (result != null && log.isDebugEnabled()) {
                log.debug("Published payment event type={} key={} topic={} partition={} offset={}",
                        eventType,
                        key,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

//    private String resolveTopic(PaymentEventType eventType) {
//        return switch (eventType) {
//            case PAYMENT_AUTHORIZED -> topicNameResolver.paymentsAuthorized();
//            case PAYMENT_CAPTURED -> topicNameResolver.paymentsCaptured();
//            default -> {
//                log.debug("Falling back to generic payments.events topic for eventType={}", eventType);
//                yield topicNameResolver.paymentsEvents();
//            }
//        };
//    }
}
