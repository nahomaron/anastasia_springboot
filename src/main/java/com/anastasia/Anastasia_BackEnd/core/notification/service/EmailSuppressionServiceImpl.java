package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionEntity;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionReason;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionSource;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.EmailSuppressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSuppressionServiceImpl implements EmailSuppressionService {

    private final EmailSuppressionRepository emailSuppressionRepository;

    @Override
    @Transactional
    public void markSuppressed(String email, EmailSuppressionReason reason, String rawNotificationType) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || reason == null) {
            return;
        }

        if (emailSuppressionRepository.existsByEmailAndReason(normalizedEmail, reason)) {
            log.debug("Email already suppressed for reason={}", reason);
            return;
        }

        EmailSuppressionEntity entity = new EmailSuppressionEntity();
        entity.setEmail(normalizedEmail);
        entity.setReason(reason);
        entity.setSource(EmailSuppressionSource.AWS_SES);
        entity.setRawNotificationType(rawNotificationType);

        try {
            emailSuppressionRepository.save(entity);
            log.info("Recorded email suppression for reason={}", reason);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Email suppression already persisted for reason={}", reason);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSuppressed(String email) {
        String normalizedEmail = normalizeEmail(email);
        return normalizedEmail != null && emailSuppressionRepository.existsByEmail(normalizedEmail);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
