package com.anastasia.Anastasia_BackEnd.core.notification.domain;


import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_user_tenant_created", columnList = "recipientUserId,tenant_id,createdAt"),
                @Index(name = "idx_notification_user_tenant_read", columnList = "recipientUserId,tenant_id,readAt"),
                @Index(name = "idx_notification_idempotency_channel", columnList = "idempotencyKey,channel")
        }
)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipientEmail;
    private String title;

    @Lob
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannelType channel;

    private boolean sent = false;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private boolean archived = false;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.PENDING;

    private UUID recipientUserId;

    private String providerMessageId;

    @Column(length = 512)
    private String errorMessage;
    @Column(length = 128)
    private String errorCode;
    @Column(length = 160)
    private String idempotencyKey;
    private Integer retryCount = 0;
    private LocalDateTime nextRetryAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private TenantEntity tenant; // ✅ Multi-tenant linkage

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
