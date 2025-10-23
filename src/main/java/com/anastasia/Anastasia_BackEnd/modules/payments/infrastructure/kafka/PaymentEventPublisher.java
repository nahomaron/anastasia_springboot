package com.anastasia.Anastasia_BackEnd.modules.payments.infrastructure.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // For skeleton simplicity we send JSON as Object; later switch to Avro types
    public void publish(String type, String tenantId, String key, JsonNode payload) {
        String topic = switch (type) {
            case "PaymentAuthorized" -> "payments.authorized";
            case "PaymentCaptured" -> "payments.captured";
            default -> "payments.events";
        };
        var record = new ProducerRecord<>(topic, key, payload.toString());
        record.headers().add("tenantId", tenantId.getBytes());
        record.headers().add("type", type.getBytes());
//        kafkaTemplate.send(record);
    }
}
