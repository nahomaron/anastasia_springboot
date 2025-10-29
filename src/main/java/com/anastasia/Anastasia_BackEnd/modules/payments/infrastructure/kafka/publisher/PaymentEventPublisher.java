package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.kafka.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String type, UUID tenantId, String key, JsonNode payload) {
        String topic = switch (type) {
            case "PaymentAuthorized" -> "payments.authorized";
            case "PaymentCaptured" -> "payments.captured";
            default -> "payments.events";
        };

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload.toString());
        String tenantHeader = tenantId != null ? tenantId.toString() : "";
        record.headers().add("tenantId", tenantHeader.getBytes());
        record.headers().add("type", type.getBytes());

        kafkaTemplate.send(record).whenComplete((SendResult<String, Object> result, Throwable ex) -> {
            if (ex != null) {
                log.error("Failed to publish payment event type={} key={}: {}", type, key, ex.getMessage());
            } else if (result != null && log.isDebugEnabled()) {
                log.debug("Published payment event type={} key={} topic={} partition={} offset={}",
                        type,
                        key,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
