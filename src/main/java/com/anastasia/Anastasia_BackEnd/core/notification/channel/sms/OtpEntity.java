package com.anastasia.Anastasia_BackEnd.core.notification.channel.sms;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "otp_codes")
public class OtpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, length = 64)
    private String otpHash;   // SHA‑256 of raw code

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int failedAttempts = 0;

    @Column
    private Instant blockedUntil;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Utility to check supplied code matches AND not expired. */
    public boolean matches(String rawOtp, String hashedRawOtpNow) {
        return hashedRawOtpNow.equals(otpHash) && Instant.now().isBefore(expiresAt);
    }

    public boolean isBlocked(Instant now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }
}
