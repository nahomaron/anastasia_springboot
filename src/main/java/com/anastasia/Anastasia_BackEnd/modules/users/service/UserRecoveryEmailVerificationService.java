package com.anastasia.Anastasia_BackEnd.modules.users.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserRecoveryEmailVerificationCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserRecoveryEmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class UserRecoveryEmailVerificationService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final UserRecoveryEmailVerificationCodeRepository repository;
    private final EmailTemplateService emailTemplateService;
    private final LocalizedMessageService messageService;

    @Transactional
    public void sendCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        String hash = hash(code);
        LocalDateTime now = LocalDateTime.now();

        UserRecoveryEmailVerificationCodeEntity entity = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(UserRecoveryEmailVerificationCodeEntity::new);
        entity.setEmail(normalizedEmail);
        entity.setCodeHash(hash);
        entity.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        entity.setVerifiedAt(null);
        entity.setAttemptCount(0);
        repository.save(entity);

        emailTemplateService.sendTemplateEmail(
                normalizedEmail,
                EmailTemplate.VERIFY_EMAIL_OTP.templateKey(),
                Map.of(
                        "userName", "User",
                        "code", code,
                        "expiresMinutes", OTP_EXPIRY_MINUTES,
                        "helpUrl", "",
                        "locale", messageService.currentLocale()
                ),
                EmailSendMetadata.of(EmailCategory.SECURITY, EmailTemplate.VERIFY_EMAIL_OTP.templateKey())
        );
    }

    @Transactional
    public boolean verifyCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = normalizeCode(code);

        UserRecoveryEmailVerificationCodeEntity entity = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "user.recoveryEmail.codeNotFound",
                        "No verification code found for this recovery email."
                )));

        if (entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException(messageService.get(
                    "auth.verificationCode.expired",
                    "Verification code has expired. Please request a new one."
            ));
        }

        entity.setAttemptCount(entity.getAttemptCount() + 1);
        boolean valid = hash(normalizedCode).equals(entity.getCodeHash());
        if (!valid) {
            repository.save(entity);
            return false;
        }

        entity.setVerifiedAt(LocalDateTime.now());
        repository.save(entity);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isVerified(String email) {
        String normalizedEmail = normalizeEmail(email);
        return repository.findByEmailIgnoreCase(normalizedEmail)
                .map(entity -> entity.getVerifiedAt() != null)
                .orElse(false);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(messageService.get("validation.user.recoveryEmail.required", "Recovery email is required."));
        }
        return email.trim().toLowerCase();
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
