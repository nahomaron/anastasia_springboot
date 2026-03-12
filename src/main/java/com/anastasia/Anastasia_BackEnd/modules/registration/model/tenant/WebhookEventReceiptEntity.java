package com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant;

import com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "webhook_event_receipts",
        indexes = {
                @Index(name = "idx_webhook_event_receipts_provider_received", columnList = "provider, received_at"),
                @Index(name = "idx_webhook_event_receipts_tenant", columnList = "tenant_id"),
                @Index(name = "idx_webhook_event_receipts_onboarding", columnList = "onboarding_session_id"),
                @Index(name = "idx_webhook_event_receipts_subscription", columnList = "tenant_subscription_id")
        }
)
public class WebhookEventReceiptEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 24)
    private String provider;

    @Column(name = "event_id", nullable = false, unique = true, length = 128)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "onboarding_session_id")
    private UUID onboardingSessionId;

    @Column(name = "tenant_subscription_id")
    private UUID tenantSubscriptionId;

    @Column(name = "event_created_at")
    private LocalDateTime eventCreatedAt;

    @Lob
    @Column(name = "payload")
    private String payload;

    @Column(name = "signature_header", length = 512)
    private String signatureHeader;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_result", nullable = false, length = 16)
    private WebhookProcessingResult processingResult;

    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.receivedAt == null) {
            this.receivedAt = now;
        }
        if (this.processingResult == null) {
            this.processingResult = WebhookProcessingResult.RETRY;
        }
        initializeAuditTimestamps(now);
    }
}
