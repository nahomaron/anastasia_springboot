package com.anastasia.Anastasia_BackEnd.modules.users.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_preferences")
public class UserPreferencesEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Builder.Default
    @Column(name = "theme_mode", nullable = false, length = 16)
    private String themeMode = "SYSTEM";

    @Builder.Default
    @Column(name = "language", nullable = false, length = 16)
    private String language = "en";

    @Builder.Default
    @Column(name = "locale", nullable = false, length = 16)
    private String locale = "en-US";

    @Builder.Default
    @Column(name = "date_format", nullable = false, length = 32)
    private String dateFormat = "MMM d, yyyy";

    @Builder.Default
    @Column(name = "first_day_of_week", nullable = false, length = 16)
    private String firstDayOfWeek = "SUNDAY";

    @Builder.Default
    @Column(name = "reduced_motion", nullable = false)
    private boolean reducedMotion = false;

    @Builder.Default
    @Column(name = "compact_ui", nullable = false)
    private boolean compactUi = false;

    @Builder.Default
    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    @Builder.Default
    @Column(name = "push_notifications", nullable = false)
    private boolean pushNotifications = true;

    @Builder.Default
    @Column(name = "marketing_notifications", nullable = false)
    private boolean marketingNotifications = false;

    @Builder.Default
    @Column(name = "share_presence", nullable = false)
    private boolean sharePresence = true;

    @Builder.Default
    @Column(name = "analytics_opt_in", nullable = false)
    private boolean analyticsOptIn = true;

    @Builder.Default
    @Column(name = "auto_detect_location", nullable = false)
    private boolean autoDetectLocation = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;
}
