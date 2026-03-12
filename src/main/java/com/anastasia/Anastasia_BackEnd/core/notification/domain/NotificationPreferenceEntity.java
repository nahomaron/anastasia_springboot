package com.anastasia.Anastasia_BackEnd.core.notification.domain;

import com.anastasia.Anastasia_BackEnd.modules.common.LocalDateTimeAuditMetadata;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "notification_preferences",
        indexes = {
                @Index(name = "idx_notification_pref_tenant_user", columnList = "tenantId,userId")
        }
)
public class NotificationPreferenceEntity extends LocalDateTimeAuditMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private boolean emailEnabled = true;

    @Column(nullable = false)
    private boolean smsEnabled = false;

    @Column(nullable = false)
    private boolean inAppEnabled = true;

    @ElementCollection(targetClass = NotificationType.class)
    @CollectionTable(name = "notification_preference_muted_types", joinColumns = @JoinColumn(name = "preference_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 64)
    private Set<NotificationType> mutedTypes = EnumSet.noneOf(NotificationType.class);

    @jakarta.persistence.PrePersist
    public void onCreate() {
        initializeAuditTimestamps(LocalDateTime.now());
    }

    @jakarta.persistence.PreUpdate
    public void onUpdate() {
        touchAuditTimestamps(LocalDateTime.now());
    }
}
