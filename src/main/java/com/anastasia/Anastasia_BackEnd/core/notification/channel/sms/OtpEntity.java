package com.anastasia.Anastasia_BackEnd.core.notification.channel.sms;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int failedAttempts = 0;

    @Column
    private LocalDateTime blockedUntil;

    /** Utility to check supplied code matches AND not expired. */
    public boolean matches(String rawOtp, String hashedRawOtpNow) {
        return hashedRawOtpNow.equals(otpHash) && LocalDateTime.now().isBefore(expiresAt);
    }

    public boolean isBlocked(LocalDateTime now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }
}
