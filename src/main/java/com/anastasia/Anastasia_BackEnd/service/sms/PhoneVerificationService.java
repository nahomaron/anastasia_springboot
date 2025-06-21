package com.anastasia.Anastasia_BackEnd.service.sms;

import com.anastasia.Anastasia_BackEnd.model.sms.OtpEntity;
import com.anastasia.Anastasia_BackEnd.repository.sms.OtpRepository;
import com.google.common.hash.Hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    Logger log = LoggerFactory.getLogger(PhoneVerificationService.class);

    private final SmsService smsService;
    private final OtpRepository otpRepository;
    private final SecureRandom random = new SecureRandom();

    private static final int OTP_EXPIRY_MINUTES = 5;

    /** Generate & send a new OTP; replaces any existing code for the phone. */
    @Transactional
    public void startVerification(String phone) {
        log.info("Phone verification started for: {}", phone);
        String otp = String.format("%06d", random.nextInt(1_000_000));
        String hash = hash(otp);

        otpRepository.findValid(phone, LocalDateTime.now())
                .ifPresent(otpRepository::delete); // revoke previous

        otpRepository.save(OtpEntity.builder()
                .phone(phone)
                .otpHash(hash)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build());

        smsService.sendSms(phone, SmsTemplateType.OTP,
                Map.of("otp_code", otp, "otp_expiry_minutes", OTP_EXPIRY_MINUTES));

    }

    /** Check code, delete on success. */
    @Transactional
    public boolean confirmOtp(String phone, String rawOtp) {
        String hash = hash(rawOtp);
        return otpRepository.findValid(phone, LocalDateTime.now())
                .filter(e -> e.matches(rawOtp, hash))
                .map(e -> { otpRepository.delete(e); return true; })
                .orElse(false);
    }

    private String hash(String text) {
        return Hashing.sha256().hashString(text, StandardCharsets.UTF_8).toString();
    }

    public void resendOtp(String phoneNumber) {
        startVerification(phoneNumber);
    }
}