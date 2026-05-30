package com.anastasia.Anastasia_BackEnd.IntegrationTest.service;

import com.anastasia.Anastasia_BackEnd.AnastasiaBackEndApplication;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.PlatformAdminRecoveryAuditEvent;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.PlatformAdminRecoveryAuditOutcome;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository.PlatformAdminRecoveryAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminRecoveryService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminRecoveryTokenResult;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = AnastasiaBackEndApplication.class)
@ActiveProfiles("test")
@Transactional
class PlatformAdminRecoveryServiceIT extends PostgresTestContainer {

    @Autowired
    private PlatformAdminRecoveryService recoveryService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PlatformAdminRecoveryAuditRepository auditRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void issueOperatorRecoveryToken_createsResetTokenRevokesExistingAndAudits() {
        UserEntity admin = saveUser("recover-admin@example.com", Set.of(requiredRole(RoleType.PLATFORM_ADMIN.name())));
        Token oldToken = tokenRepository.save(Token.builder()
                .token("old-token-hash")
                .tokenType(TokenType.PASSWORD_RESET)
                .createdAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(600))
                .user(admin)
                .expired(false)
                .revoked(false)
                .build());

        PlatformAdminRecoveryTokenResult result = recoveryService.issueOperatorRecoveryToken(
                admin.getEmail(),
                "ops-user",
                "lost access"
        );

        entityManager.flush();
        entityManager.clear();

        UserEntity reloaded = userRepository.findById(admin.getUuid()).orElseThrow();
        assertThat(reloaded.isMustChangePassword()).isTrue();
        assertThat(reloaded.getTemporaryPasswordIssuedAt()).isNotNull();
        assertThat(result.resetUrl()).contains("/reset-password?token=");
        assertThat(result.expiresAt()).isAfter(Instant.now());

        Token revokedOldToken = tokenRepository.findById(oldToken.getId()).orElseThrow();
        assertThat(revokedOldToken.isExpired()).isTrue();
        assertThat(revokedOldToken.isRevoked()).isTrue();

        Token newToken = tokenRepository.findByUserUuidAndTokenTypeOrderByIdDesc(admin.getUuid(), TokenType.PASSWORD_RESET).get(0);
        assertThat(newToken.getId()).isNotEqualTo(oldToken.getId());
        assertThat(result.resetUrl()).doesNotContain(newToken.getToken());
        assertThat(result.expiresAt()).isEqualTo(newToken.getExpiresAt());

        PlatformAdminRecoveryAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminRecoveryAuditOutcome.SUCCESS);
        assertThat(event.getTargetUserId()).isEqualTo(admin.getUuid());
        assertThat(event.getIssuedTokenId()).isEqualTo(newToken.getId());
        assertThat(event.getOperatorName()).isEqualTo("ops-user");
    }

    @Test
    void issueOperatorRecoveryToken_rejectsNonPlatformAdminAndAudits() {
        UserEntity user = saveUser("plain-user@example.com", Set.of(requiredRole(RoleType.USER.name())));

        assertThatThrownBy(() -> recoveryService.issueOperatorRecoveryToken(user.getEmail(), "ops-user", "reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Recovery can only be issued for a platform admin account.");

        PlatformAdminRecoveryAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminRecoveryAuditOutcome.NOT_PLATFORM_ADMIN);
        assertThat(event.getTargetUserId()).isEqualTo(user.getUuid());
    }

    @Test
    void issueOperatorRecoveryToken_rejectsMissingUserAndAudits() {
        assertThatThrownBy(() -> recoveryService.issueOperatorRecoveryToken("missing@example.com", "ops-user", "reason"))
                .isInstanceOf(EntityNotFoundException.class);

        PlatformAdminRecoveryAuditEvent event = latestAuditEvent();
        assertThat(event.getOutcome()).isEqualTo(PlatformAdminRecoveryAuditOutcome.USER_NOT_FOUND);
        assertThat(event.getAttemptedEmail()).isEqualTo("missing@example.com");
    }

    private UserEntity saveUser(String email, Set<Role> roles) {
        UserEntity user = UserEntity.builder()
                .fullName("Recovery User")
                .email(email)
                .password("encoded-password")
                .userType(UserType.STAFF)
                .status(UserStatus.ACTIVE)
                .roles(new LinkedHashSet<>(roles))
                .build();
        user.setVerified(true);
        return userRepository.save(user);
    }

    private Role requiredRole(String roleName) {
        return roleRepository.findByRoleName(roleName).orElseThrow();
    }

    private PlatformAdminRecoveryAuditEvent latestAuditEvent() {
        return auditRepository.findAll().stream()
                .max(Comparator.comparing(PlatformAdminRecoveryAuditEvent::getId))
                .orElseThrow();
    }
}
