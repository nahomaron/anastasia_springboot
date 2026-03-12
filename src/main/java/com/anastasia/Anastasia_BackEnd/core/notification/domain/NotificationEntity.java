package com.anastasia.Anastasia_BackEnd.core.notification.domain;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user_tenant_created", columnList = "recipient_user_id,tenant_id,created_at"),
                @Index(name = "idx_notification_user_tenant_read", columnList = "recipient_user_id,tenant_id,read_at"),
                @Index(name = "idx_notification_tenant_channel_status", columnList = "tenant_id,channel,delivery_status"),
                @Index(name = "idx_notification_idempotency_channel", columnList = "idempotency_key,channel")
        }
)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_address", length = 320)
    private String recipientAddress;

    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    private String title;

    @Lob
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannelType channel;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 24)
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.PENDING;

    @Column(name = "provider_name", length = 64)
    private String provider;

    @Column(length = 128)
    private String providerStatus;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "correlation_id", length = 160)
    private String correlationId;

    @Column(length = 512)
    private String errorMessage;

    @Column(length = 128)
    private String errorCode;

    @Column(name = "idempotency_key", length = 160)
    private String idempotencyKey;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant;

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private UUID tenantId;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getRecipientEmail() {
        return recipientAddress;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientAddress = recipientEmail;
    }

    public boolean isSent() {
        return deliveryStatus == NotificationDeliveryStatus.SENT;
    }

    public void setSent(boolean sent) {
        if (sent) {
            this.deliveryStatus = NotificationDeliveryStatus.SENT;
            if (deliveredAt == null) {
                deliveredAt = Instant.now();
            }
            this.failedAt = null;
        } else if (deliveryStatus == NotificationDeliveryStatus.SENT) {
            this.deliveryStatus = NotificationDeliveryStatus.FAILED;
            if (failedAt == null) {
                failedAt = Instant.now();
            }
        }
    }

    public Instant getSentAt() {
        return deliveredAt;
    }

    public void setSentAt(Instant sentAt) {
        this.deliveredAt = sentAt;
        if (sentAt != null) {
            this.deliveryStatus = NotificationDeliveryStatus.SENT;
            this.failedAt = null;
        }
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public void setArchived(boolean archived) {
        this.archivedAt = archived ? Instant.now() : null;
    }
}
