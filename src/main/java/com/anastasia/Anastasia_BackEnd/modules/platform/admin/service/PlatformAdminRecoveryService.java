package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import com.anastasia.Anastasia_BackEnd.common.config.PublicUrlUtils;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.IssuedPasswordResetToken;
import com.anastasia.Anastasia_BackEnd.core.auth.service.PasswordResetTokenService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.PlatformAdminRecoveryAuditEvent;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.PlatformAdminRecoveryAuditOutcome;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository.PlatformAdminRecoveryAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlatformAdminRecoveryService {

    private static final String BREAK_GLASS_ROLE = "DEVELOPER_SUPER_USER";
    private static final Set<String> PLATFORM_ADMIN_ROLE_NAMES = Set.of(
            RoleType.PLATFORM_ADMIN.name(),
            BREAK_GLASS_ROLE
    );

    @Value("${app.public.frontend-base-url:}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final AuthService authService;
    private final PlatformAdminRecoveryAuditRepository auditRepository;

    @Transactional
    public PlatformAdminRecoveryTokenResult issueOperatorRecoveryToken(String email, String operatorName, String reason) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedOperator = normalize(operatorName, 120);
        String normalizedReason = normalize(reason, 512);

        try {
            UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Platform admin account not found for recovery."));

            if (!isPlatformAdmin(user)) {
                audit(normalizedEmail, user, null, normalizedOperator, normalizedReason,
                        PlatformAdminRecoveryAuditOutcome.NOT_PLATFORM_ADMIN,
                        "Requested user is not a platform admin.");
                throw new IllegalArgumentException("Recovery can only be issued for a platform admin account.");
            }

            authService.revokeAllActiveUserTokens(user);
            user.setMustChangePassword(true);
            user.setTemporaryPasswordIssuedAt(Instant.now());
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);

            IssuedPasswordResetToken issuedToken = passwordResetTokenService.issueForUser(user);
            UserEntity saved = userRepository.save(user);

            audit(normalizedEmail, saved, issuedToken.tokenId(), normalizedOperator, normalizedReason,
                    PlatformAdminRecoveryAuditOutcome.SUCCESS,
                    "Operator-issued platform admin recovery token created.");

            return new PlatformAdminRecoveryTokenResult(
                    saved.getEmail(),
                    saved.getUuid(),
                    normalizeBaseUrl(frontendBaseUrl) + "/reset-password?token=" + issuedToken.rawToken(),
                    issuedToken.expiresAt()
            );
        } catch (EntityNotFoundException ex) {
            audit(normalizedEmail, null, null, normalizedOperator, normalizedReason,
                    PlatformAdminRecoveryAuditOutcome.USER_NOT_FOUND, ex.getMessage());
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            audit(normalizedEmail, null, null, normalizedOperator, normalizedReason,
                    PlatformAdminRecoveryAuditOutcome.FAILED, ex.getMessage());
            throw ex;
        }
    }

    private boolean isPlatformAdmin(UserEntity user) {
        return user.getRoles() != null && user.getRoles().stream()
                .map(Role::getRoleName)
                .anyMatch(PLATFORM_ADMIN_ROLE_NAMES::contains);
    }

    private void audit(
            String attemptedEmail,
            UserEntity user,
            Integer issuedTokenId,
            String operatorName,
            String reason,
            PlatformAdminRecoveryAuditOutcome outcome,
            String detail
    ) {
        PlatformAdminRecoveryAuditEvent event = new PlatformAdminRecoveryAuditEvent();
        event.setAttemptedEmail(attemptedEmail);
        event.setTargetUserId(user != null ? user.getUuid() : null);
        event.setIssuedTokenId(issuedTokenId);
        event.setOperatorName(operatorName);
        event.setReason(reason);
        event.setOutcome(outcome);
        event.setDetail(normalize(detail, 512));
        event.setOccurredAt(Instant.now());
        auditRepository.save(event);
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Recovery email is required.");
        }
        return normalized;
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String normalizeBaseUrl(String rawUrl) {
        return PublicUrlUtils.normalizeBaseUrl(rawUrl, "app.public.frontend-base-url");
    }
}
