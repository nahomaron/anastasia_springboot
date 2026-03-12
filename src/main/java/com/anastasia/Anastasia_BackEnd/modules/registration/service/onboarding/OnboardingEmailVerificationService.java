package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.OnboardingEmailVerificationCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.OnboardingEmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OnboardingEmailVerificationService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 10;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final OnboardingEmailVerificationCodeRepository repository;
    private final EmailTemplateService emailTemplateService;
    private final LocalizedMessageService messageService;

    @Value("${app.onboarding.verification.help-url:https://app.anastasia.com/help/security}")
    private String helpUrl;

    @Transactional
    public void sendCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String hash = hash(code);
        Instant now = Instant.now();

        OnboardingEmailVerificationCodeEntity entity = repository.findByEmailIgnoreCase(email)
                .orElseGet(OnboardingEmailVerificationCodeEntity::new);
        entity.setEmail(email);
        entity.setCodeHash(hash);
        entity.setExpiresAt(now.plusSeconds(OTP_EXPIRY_MINUTES * 60L));
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
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "auth.verificationCode.notFound",
                        "No verification code found for this email."
                )));

        Instant now = Instant.now();
        if (entity.getBlockedUntil() != null && entity.getBlockedUntil().isAfter(now)) {
            throw new IllegalStateException(messageService.get(
                    "auth.verificationCode.locked",
                    "Too many invalid attempts. Please try again later."
            ));
        }
        if (entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(now)) {
            throw new IllegalStateException(messageService.get(
                    "auth.verificationCode.expiredNewCode",
                    "Verification code has expired. Please request a new code."
            ));
        }

        entity.setAttemptCount(entity.getAttemptCount() + 1);
        entity.setLastAttemptAt(now);
        boolean valid = hash(code).equals(entity.getCodeHash());
        if (!valid) {
            if (entity.getAttemptCount() >= MAX_ATTEMPTS) {
                entity.setBlockedUntil(now.plusSeconds(LOCKOUT_MINUTES * 60L));
            }
            repository.save(entity);
            return false;
        }

        entity.setVerifiedAt(now);
        entity.setBlockedUntil(null);
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
        Map<String, Object> model = Map.of(
                "userName", "Church Admin",
                "code", code,
                "expiresMinutes", OTP_EXPIRY_MINUTES,
                "helpUrl", helpUrl,
                "locale", messageService.currentLocale()
        );

        emailTemplateService.sendTemplateEmail(
                to,
                EmailTemplate.VERIFY_EMAIL_OTP.templateKey(),
                model,
                EmailSendMetadata.of(EmailCategory.SECURITY, EmailTemplate.VERIFY_EMAIL_OTP.templateKey())
        );
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(messageService.get("validation.auth.email.required", "Email is required."));
        }
        String normalized = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(messageService.get("validation.auth.email.invalid", "Email format is invalid."));
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        if (code == null || !code.trim().matches("^\\d{6}$")) {
            throw new IllegalArgumentException(messageService.get(
                    "validation.auth.verificationCode.sixDigits",
                    "Verification code must be 6 digits."
            ));
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
