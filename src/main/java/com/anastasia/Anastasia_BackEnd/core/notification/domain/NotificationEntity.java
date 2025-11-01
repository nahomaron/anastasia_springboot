package com.anastasia.Anastasia_BackEnd.core.notification.domain;


import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notifications")
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

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String providerMessageId;

    @Column(length = 512)
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
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
