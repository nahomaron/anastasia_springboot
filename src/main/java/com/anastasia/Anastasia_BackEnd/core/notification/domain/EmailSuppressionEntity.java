package com.anastasia.Anastasia_BackEnd.core.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "email_suppressions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_email_suppressions_email_reason", columnNames = {"email", "reason"})
        },
        indexes = {
                @Index(name = "idx_email_suppressions_email", columnList = "email"),
                @Index(name = "idx_email_suppressions_created_at", columnList = "created_at")
        }
)
public class EmailSuppressionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EmailSuppressionReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EmailSuppressionSource source;

    @Column(name = "raw_notification_type", length = 64)
    private String rawNotificationType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
