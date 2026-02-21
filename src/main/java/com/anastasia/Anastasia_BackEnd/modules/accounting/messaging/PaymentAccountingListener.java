package com.anastasia.Anastasia_BackEnd.modules.accounting.messaging;

import com.anastasia.Anastasia_BackEnd.modules.accounting.dto.PaymentCapturedMessage;
import com.anastasia.Anastasia_BackEnd.modules.accounting.service.TransactionService;
import com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaConsumerGroupNames;
import com.anastasia.Anastasia_BackEnd.core.kafka.util.KafkaHeaderNames;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class PaymentAccountingListener {

    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;

    @KafkaListener(
            topics = "#{@kafkaTopicNameResolver.paymentsCaptured()}",
            groupId = KafkaConsumerGroupNames.ACCOUNTING
    )
    public void handlePaymentCaptured(ConsumerRecord<String, String> record) {
        try {
            PaymentCapturedMessage message = mapRecord(record);
            transactionService.recordPaymentCapture(message);
        } catch (Exception ex) {
            log.error("Failed to record captured payment in accounting. key={} error={}", record.key(), ex.getMessage(), ex);
            // Let the container's error handler decide on retries / DLQ.
            throw new IllegalStateException("Accounting posting failed", ex);
        }
    }

    private PaymentCapturedMessage mapRecord(ConsumerRecord<String, String> record) throws Exception {
        JsonNode payload = objectMapper.readTree(record.value());

        UUID tenantId = extractTenantId(record, payload);
        if (tenantId == null) {
            throw new IllegalArgumentException("Captured payment payload missing tenantId");
        }

        return PaymentCapturedMessage.builder()
                .tenantId(tenantId)
                .paymentId(getText(payload, "paymentId"))
                .providerRef(getText(payload, "providerRef"))
                .purpose(getText(payload, "purpose"))
                .currency(getText(payload, "currency"))
                .grossAmountMinor(getLong(payload, "gross"))
                .netAmountMinor(getLong(payload, "net"))
                .feeAmountMinor(getLong(payload, "fees"))
                .fundId(getText(payload, "fundId"))
                .memberId(getText(payload, "memberId"))
                .capturedAt(parseInstant(getText(payload, "capturedAt")))
                .build();
    }

    private UUID extractTenantId(ConsumerRecord<String, String> record, JsonNode payload) {
        String payloadTenant = getText(payload, "tenantId");
        if (payloadTenant != null && !payloadTenant.isBlank()) {
            return parseTenantId(payloadTenant);
        }
        Header header = record.headers().lastHeader(KafkaHeaderNames.TENANT_ID);
        if (header != null) {
            return parseTenantId(new String(header.value(), StandardCharsets.UTF_8));
        }
        return null;
    }

    private UUID parseTenantId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid tenantId supplied for accounting event: " + raw, ex);
        }
    }

    private String getText(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asText();
    }

    private Long getLong(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull()) {
            return null;
        }
        try {
            return child.isNumber() ? child.longValue() : Long.parseLong(child.asText());
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse numeric field '{}' from payload: {}", fieldName, child.asText());
            return null;
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            log.warn("Unable to parse instant '{}'", value);
            return null;
        }
    }
}
