package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingEmailVerificationCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.OnboardingEmailVerificationCodeRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OnboardingEmailVerificationService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final OnboardingEmailVerificationCodeRepository repository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:info@anastasia.com}")
    private String sender;

    @Transactional
    public void sendCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String hash = hash(code);
        LocalDateTime now = LocalDateTime.now();

        OnboardingEmailVerificationCodeEntity entity = repository.findByEmailIgnoreCase(email)
                .orElseGet(OnboardingEmailVerificationCodeEntity::new);
        entity.setEmail(email);
        entity.setCodeHash(hash);
        entity.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        entity.setVerifiedAt(null);
        entity.setAttemptCount(0);
        repository.save(entity);

        sendEmail(email, code);
    }

    @Transactional
    public boolean verifyCode(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String code = normalizeCode(rawCode);

        OnboardingEmailVerificationCodeEntity entity = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("No verification code found for this email."));

        if (entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Verification code has expired. Please request a new code.");
        }

        entity.setAttemptCount(entity.getAttemptCount() + 1);
        boolean valid = hash(code).equals(entity.getCodeHash());
        if (!valid) {
            repository.save(entity);
            return false;
        }

        entity.setVerifiedAt(LocalDateTime.now());
        repository.save(entity);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isVerified(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        return repository.findByEmailIgnoreCase(email)
                .map(entity -> entity.getVerifiedAt() != null)
                .orElse(false);
    }

    private void sendEmail(String to, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setFrom(sender);
            helper.setSubject("Verify your email for Anastasia onboarding");
            helper.setText("""
                    Your verification code is: %s

                    This code expires in %d minutes.
                    """.formatted(code, OTP_EXPIRY_MINUTES), false);
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send verification email", ex);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email format is invalid.");
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        if (code == null || !code.trim().matches("^\\d{6}$")) {
            throw new IllegalArgumentException("Verification code must be 6 digits.");
        }
        return code.trim();
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash verification code", e);
        }
    }
}
