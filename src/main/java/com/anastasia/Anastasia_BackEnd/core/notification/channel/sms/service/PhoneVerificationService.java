package com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service;

import com.anastasia.Anastasia_BackEnd.common.utils.PhoneNumberUtils;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.OtpEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationChannelType;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationEvent;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.NotificationType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.OtpRepository;
import com.google.common.hash.Hashing;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);

    private final OtpRepository otpRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 10;

    /** Generate & send a new OTP; replaces any existing code for the phone. */
    @Transactional
    public void startVerification(String phone) {
        String normalizedPhone = PhoneNumberUtils.normalize(phone);
        log.info("Phone verification started for: {}", PhoneNumberUtils.mask(normalizedPhone));
        String otp = String.format("%06d", random.nextInt(1_000_000));
        String hash = hash(otp);
        LocalDateTime now = LocalDateTime.now();

        otpRepository.findValid(normalizedPhone, now)
                .ifPresent(otpRepository::delete); // revoke previous

        otpRepository.save(OtpEntity.builder()
                .phone(normalizedPhone)
                .otpHash(hash)
                .expiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES))
                .failedAttempts(0)
                .blockedUntil(null)
                .build());

        Map<String, Object> properties = Map.of(
                "phone", normalizedPhone,
                "otp_code", otp,
                "otp_expiry_minutes", OTP_EXPIRY_MINUTES
        );

        eventPublisher.publishEvent(
                new NotificationEvent(
                        this,
                        NotificationType.PHONE_VERIFICATION,
                        null,
                        properties,
                        EnumSet.of(NotificationChannelType.SMS))
        );
    }

    /** Check code, delete on success. */
    @Transactional
    public boolean confirmOtp(String phone, String rawOtp) {
        String normalizedPhone = PhoneNumberUtils.normalize(phone);
        LocalDateTime now = LocalDateTime.now();
        String hash = hash(rawOtp);

        OtpEntity otpEntity = otpRepository.findValid(normalizedPhone, now).orElse(null);
        if (otpEntity == null) {
            return false;
        }

        if (otpEntity.isBlocked(now)) {
            return false;
        }

        if (otpEntity.matches(rawOtp, hash)) {
            otpRepository.delete(otpEntity);
            return true;
        }

        int failed = otpEntity.getFailedAttempts() + 1;
        otpEntity.setFailedAttempts(failed);
        if (failed >= MAX_FAILED_ATTEMPTS) {
            otpEntity.setBlockedUntil(now.plusMinutes(LOCKOUT_MINUTES));
        }
        otpRepository.save(otpEntity);
        return false;
    }

    private String hash(String text) {
        return Hashing.sha256().hashString(text, StandardCharsets.UTF_8).toString();
    }

    public void resendOtp(String phoneNumber) {
        startVerification(phoneNumber);
    }

    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        int deleted = otpRepository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.debug("Deleted {} expired OTP entries", deleted);
        }
    }
}
