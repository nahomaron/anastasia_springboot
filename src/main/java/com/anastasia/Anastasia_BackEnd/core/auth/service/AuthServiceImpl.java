package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.AuthenticationProcessException;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.InvalidCredentialsException;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthSessionResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.VerifyLoginTwoFactorRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.PermissionType;
import com.anastasia.Anastasia_BackEnd.core.auth.model.LoginTwoFactorChallengeEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.LoginTwoFactorChallengeRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.common.cache.CacheWarmupService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailSendMetadata;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplate;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserPreferencesEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserTwoFactorBackupCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserPreferencesRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserTwoFactorBackupCodeRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.security.TotpUtils;
import jakarta.mail.IllegalWriteException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${app.public.frontend-base-url:}")
    private String frontendBaseUrl;

    private final JwtUtil jwtUtil;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailTemplateService emailTemplateService;
    private final LocalizedMessageService messageService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final CacheWarmupService cacheWarmupService;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserTwoFactorBackupCodeRepository backupCodeRepository;
    private final LoginTwoFactorChallengeRepository loginTwoFactorChallengeRepository;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final TenantRepository tenantRepository;
    private final StaffRepository staffRepository;
    private final MemberEffectivePermissionService memberEffectivePermissionService;

    private static final int LOGIN_2FA_MAX_ATTEMPTS = 5;
    private static final int LOGIN_2FA_CHALLENGE_MINUTES = 10;

    @Override
    public void createUser(UserEntity userEntity) throws MessagingException {
        // todo -> make role fetching and assigning method

        if (userRepository.existsByEmail(userEntity.getEmail())) {
            throw new IllegalWriteException("The provided email is already in use. Please use a different email.");
        }

        try {

            userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
            if (userEntity.getUserType() == null) {
                userEntity.setUserType(UserType.GUEST);
            }
            attachTenantFromContextIfMissing(userEntity);

            if (userEntity.getRoles() == null || userEntity.getRoles().isEmpty()) {
                Role userRole = roleRepository.findByRoleName("USER")
                        .orElseThrow(() -> new RuntimeException("User role not found"));
                userEntity.setRoles(Set.of(userRole));
            }
            UserEntity savedUser = userRepository.save(userEntity);


            // Only send email if save was successful and no exceptions occurred
            sendValidationEmail(savedUser);

        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error creating user: " + e.getMessage());
            throw new RuntimeException(messageService.get("auth.user.creationFailed", "User creation failed: {0}", e.getMessage()));
        }

    }

    private void attachTenantFromContextIfMissing(UserEntity userEntity) {
        if (userEntity == null || userEntity.getTenant() != null) {
            return;
        }

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return;
        }

        TenantEntity tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant != null) {
            userEntity.assignAffiliatedTenant(tenant);
        }
    }

    @Override
    @Transactional
    public AuthenticationResponse activateAccount(String token, String email) {
        String normalizedEmail = normalizeEmail(email);
        Instant now = Instant.now();

        Token savedToken = tokenRepository.findActiveTokensByValueAndType(token, TokenType.ACTIVATION).stream()
                .filter(candidate -> candidate.getUser() != null
                        && candidate.getUser().getEmail() != null
                        && candidate.getUser().getEmail().trim().equalsIgnoreCase(normalizedEmail))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(messageService.get("auth.activation.invalidToken", "Invalid activation token")));

        if (savedToken.getTokenType() != TokenType.ACTIVATION) {
            throw new RuntimeException(messageService.get("auth.activation.invalidToken", "Invalid activation token"));
        }

        if (savedToken.getValidatedAt() != null || savedToken.isExpired() || savedToken.isRevoked()) {
            throw new RuntimeException(messageService.get("auth.activation.invalidToken", "Invalid activation token"));
        }

        if (savedToken.getExpiresAt() == null || !savedToken.getExpiresAt().isAfter(now)) {
            savedToken.setExpired(true);
            savedToken.setExpiredAt(now);
            tokenRepository.save(savedToken);
            throw new RuntimeException(messageService.get("auth.activation.expiredToken", "Activation token has expired. Please request a new activation email."));
        }

        var user = userRepository.findById(savedToken.getUser().getUuid())
                .orElseThrow(() -> new UsernameNotFoundException("Activation - Username not found"));

        if (user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(normalizedEmail)) {
            throw new RuntimeException(messageService.get("auth.activation.invalidToken", "Invalid activation token"));
        }

        if (!user.isVerified()) {
            user.setVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }
        savedToken.setValidatedAt(now);
        savedToken.setExpired(true);
        savedToken.setExpiredAt(now);
        tokenRepository.save(savedToken);

        return issueSessionForUser(user.getUuid());
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws MessagingException {
        Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent() && !existingUser.get().isVerified()) {
            handleUnverifiedLoginAttempt(existingUser.get());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new InvalidCredentialsException(messageService.get("auth.login.invalidCredentials", "Unauthorized: Invalid email or password"));
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationProcessException(
                    messageService.get("auth.login.unexpectedError", "An unexpected error occurred during login"),
                    e
            );
        }


        var user = existingUser.orElseGet(() -> userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException(
                        messageService.get("auth.login.userNotFound", "Login - Username not found")
                )));

        if (isTwoFactorRequired(user)) {
            return createTwoFactorChallenge(user);
        }

        return issueSessionForUser(user.getUuid());
    }

    private void handleUnverifiedLoginAttempt(UserEntity user) throws MessagingException {
        if (user.getCreatedAt() != null && user.getCreatedAt().isBefore(Instant.now().minusSeconds(24L * 60L * 60L))) {
            sendValidationEmail(user);
            throw new IllegalStateException(messageService.get(
                    "auth.login.accountNotVerifiedResent",
                    "Login: Account is not verified. Please find a new token sent to you for verification!"
            ));
        }
        throw new IllegalStateException(messageService.get(
                "auth.login.accountNotVerified",
                "Login: Account is not verified. Please find the token sent to you for verification!"
        ));
    }

    @Override
    public AuthenticationResponse authenticateGoogleUser(String googleId, String email, String fullName) {
        String normalizedGoogleId = googleId == null ? null : googleId.trim();
        String normalizedEmail = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        String resolvedName = fullName == null ? null : fullName.trim();

        if (normalizedGoogleId == null || normalizedGoogleId.isBlank()) {
            throw new IllegalArgumentException(messageService.get("auth.google.idMissing", "Google account id is missing."));
        }
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException(messageService.get("auth.google.emailMissing", "Google account email is missing."));
        }

        UserEntity user = userRepository.findByGoogleId(normalizedGoogleId)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null));

        if (user == null) {
            Role userRole = roleRepository.findByRoleName("USER")
                    .orElseThrow(() -> new IllegalStateException("User role not found"));

            String fallbackName = resolvedName;
            if (fallbackName == null || fallbackName.isBlank()) {
                fallbackName = normalizedEmail.substring(0, normalizedEmail.indexOf('@'));
            }

            user = UserEntity.builder()
                    .googleId(normalizedGoogleId)
                    .email(normalizedEmail)
                    .fullName(fallbackName)
                    .emailVerifiedAt(Instant.now())
                    .status(UserStatus.ACTIVE)
                    .userType(UserType.GUEST)
                    .roles(Set.of(userRole))
                    .build();
            attachTenantFromContextIfMissing(user);
        } else {
            if (user.getGoogleId() != null && !user.getGoogleId().equals(normalizedGoogleId)) {
                throw new IllegalStateException(messageService.get(
                        "auth.google.emailLinkedElsewhere",
                        "This email is already linked to a different Google account."
                ));
            }

            user.setGoogleId(normalizedGoogleId);
            user.setEmail(normalizedEmail);
            if (resolvedName != null && !resolvedName.isBlank()) {
                user.setFullName(resolvedName);
            }
            if (!user.isVerified()) {
                user.setVerified(true);
                user.setStatus(UserStatus.ACTIVE);
            }
            attachTenantFromContextIfMissing(user);
        }

        UserEntity savedUser = userRepository.save(user);
        ensureUserProfileExists(savedUser);
        ensureUserPreferencesExist(savedUser);

        if (isTwoFactorRequired(savedUser)) {
            return createTwoFactorChallenge(savedUser);
        }

        return issueSessionForUser(savedUser.getUuid());
    }

    @Override
    public AuthenticationResponse verifyLoginTwoFactor(VerifyLoginTwoFactorRequest request) {
        String challengeToken = request.getChallengeToken().trim();
        LoginTwoFactorChallengeEntity challenge = loginTwoFactorChallengeRepository.findByChallengeToken(challengeToken)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get("auth.twoFactor.challenge.invalid", "Invalid two-factor challenge.")));

        if (challenge.getConsumedAt() != null) {
            throw new IllegalArgumentException(messageService.get("auth.twoFactor.challenge.used", "Two-factor challenge already used."));
        }
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException(messageService.get("auth.twoFactor.challenge.expired", "Two-factor challenge expired. Please login again."));
        }
        if (challenge.getAttemptCount() >= LOGIN_2FA_MAX_ATTEMPTS) {
            throw new IllegalStateException(messageService.get("auth.twoFactor.challenge.tooManyAttempts", "Too many invalid two-factor attempts. Please login again."));
        }

        UserEntity user = challenge.getUser();
        UserProfileEntity profile = userProfileRepository.findById(user.getUuid())
                .orElseThrow(() -> new IllegalStateException(messageService.get("auth.twoFactor.profileMissing", "Two-factor profile is missing.")));

        String input = request.getCode() == null ? "" : request.getCode().trim();
        boolean valid = verifyTwoFactorInput(profile, user, input);

        challenge.setAttemptCount(challenge.getAttemptCount() + 1);
        challenge.setLastAttemptAt(Instant.now());
        if (!valid) {
            loginTwoFactorChallengeRepository.save(challenge);
            throw new IllegalArgumentException(messageService.get("auth.verificationCode.invalid", "Invalid verification code."));
        }

        challenge.setConsumedAt(Instant.now());
        loginTwoFactorChallengeRepository.save(challenge);
        return issueSessionForUser(user.getUuid());
    }

    @Override
    public AuthenticationResponse issueSessionForUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Session issue failed - user not found"));

        if (user.isAccountLocked()) {
            throw new IllegalStateException(messageService.get("auth.account.locked", "User account is locked."));
        }
        if (!user.isVerified()) {
            throw new IllegalStateException(messageService.get("auth.account.notVerified", "User account is not verified."));
        }
        boolean showWelcomeMessage = user.getLastLoginAt() == null;
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        touchStaffLoginAudit(user);

        UserPrincipal userPrincipal = new UserPrincipal(
                user,
                resolveEffectiveRoles(user),
                memberEffectivePermissionService.resolvePermissions(user)
        );
        AuthSessionResponse session = buildAuthSessionResponse(user, showWelcomeMessage);
        String sessionId = UUID.randomUUID().toString();
        String accessJwtId = UUID.randomUUID().toString();
        String refreshJwtId = UUID.randomUUID().toString();

        var jwtToken = jwtUtil.generateAccessToken(userPrincipal, sessionId, accessJwtId);
        var refreshToken = jwtUtil.generateRefreshToken(userPrincipal, sessionId, refreshJwtId);

        revokeAllActiveUserTokens(user);
        saveUserToken(jwtToken, user, TokenType.BEARER, sessionId, accessJwtId);
        saveUserToken(refreshToken, user, TokenType.REFRESH, sessionId, refreshJwtId);

        if (userPrincipal.getRoleNames().stream().anyMatch(role -> "ADMIN".equals(role) || "PRIMARY_ADMIN".equals(role))
                && user.getTenant() != null) {
            UUID tenantId = user.getTenant().getId();
            cacheWarmupService.warmUpTenantCache(tenantId);
        }

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .session(session)
                .build();
    }

    private void touchStaffLoginAudit(UserEntity user) {
        StaffEntity staffProfile = user.getStaffProfile();
        if (staffProfile == null) {
            return;
        }

        Instant now = Instant.now();
        boolean changed = false;

        if (staffProfile.getInviteAcceptedAt() == null) {
            staffProfile.setInviteAcceptedAt(now);
            changed = true;
        }
        if (staffProfile.getFirstLoginAt() == null) {
            staffProfile.setFirstLoginAt(now);
            changed = true;
        }

        if (changed) {
            staffRepository.save(staffProfile);
        }
    }

    private void ensureUserProfileExists(UserEntity user) {
        if (user == null || user.getUuid() == null || userProfileRepository.findById(user.getUuid()).isPresent()) {
            return;
        }

        userProfileRepository.save(UserProfileEntity.builder()
                .user(user)
                .phoneVerified(false)
                .twoFactorEnabled(false)
                .build());
    }

    private void ensureUserPreferencesExist(UserEntity user) {
        if (user == null || user.getUuid() == null || userPreferencesRepository.findById(user.getUuid()).isPresent()) {
            return;
        }

        userPreferencesRepository.save(UserPreferencesEntity.builder()
                .user(user)
                .themeMode("SYSTEM")
                .language("en")
                .locale("en-US")
                .dateFormat("MMM d, yyyy")
                .firstDayOfWeek("SUNDAY")
                .emailNotifications(true)
                .pushNotifications(true)
                .marketingNotifications(false)
                .sharePresence(true)
                .analyticsOptIn(true)
                .autoDetectLocation(true)
                .build());
    }

    @Override
    public AuthenticationResponse refreshToken(HttpServletRequest request) {
        String refreshToken = refreshTokenCookieService.extractRefreshToken(request)
                .orElseThrow(() -> new IllegalArgumentException(messageService.get("auth.refreshToken.missing", "Refresh token cookie is missing.")));
        Token storedRefreshToken = tokenRepository.findActiveTokensByValueAndType(refreshToken, TokenType.REFRESH).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(messageService.get(
                        "auth.refreshToken.invalid",
                        "Refresh token is invalid or expired."
                )));

        String username = jwtUtil.extractUsername(refreshToken);

        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid, User doesn't exist"));

        if (!storedRefreshToken.getUser().getUuid().equals(user.getUuid())) {
            throw new IllegalArgumentException(messageService.get("auth.refreshToken.invalid", "Refresh token is invalid or expired."));
        }

        String refreshJwtId = jwtUtil.extractJwtId(refreshToken);
        if (storedRefreshToken.getJwtId() == null || !storedRefreshToken.getJwtId().equals(refreshJwtId)) {
            throw new IllegalArgumentException(messageService.get("auth.refreshToken.invalid", "Refresh token is invalid or expired."));
        }

        UserPrincipal userPrincipal = new UserPrincipal(
                user,
                resolveEffectiveRoles(user),
                memberEffectivePermissionService.resolvePermissions(user)
        );

        if (!jwtUtil.isTokenValid(refreshToken, userPrincipal)) {
            throw new IllegalArgumentException(messageService.get("auth.refreshToken.invalid", "Refresh token is invalid or expired."));
        }

        String sessionId = storedRefreshToken.getSessionId();
        revokeActiveSessionBearerTokens(user.getUuid(), sessionId);

        String accessJwtId = UUID.randomUUID().toString();
        var accessToken = jwtUtil.generateAccessToken(userPrincipal, sessionId, accessJwtId);
        saveUserToken(accessToken, user, TokenType.BEARER, sessionId, accessJwtId);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .session(buildAuthSessionResponse(user, false))
                .build();
    }

    @Override
    public Optional<UserEntity> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void resendActivationEmail(String email) throws MessagingException {
        // Find the user by email
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist"));

        // Check if the user is already activated
        if (user.isVerified()) {
            throw new IllegalStateException(messageService.get("auth.account.alreadyVerified", "User is already verified"));
        }

        // Generate and send a new activation email
        sendValidationEmail(user);
    }

    public void initiatePasswordReset(String email) throws MessagingException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        messageService.get("auth.passwordReset.userNotFound", "User not found with email: {0}", email)
                ));

        String resetTokenValue = generateActivationCode(8); // Longer token for security
        // Invalidate any existing password reset tokens for this user first (optional but good practice)
        tokenRepository.findAllValidTokensByUser(user.getUuid(), TokenType.PASSWORD_RESET) // Assuming you add TokenType.PASSWORD_RESET
                .forEach(token -> {
                    token.setExpired(true);
                    token.setRevoked(true);
                    token.setExpiredAt(Instant.now());
                    token.setRevokedAt(Instant.now());
                });

        Token resetToken = Token.builder()
                .token(resetTokenValue)
                .tokenType(TokenType.PASSWORD_RESET) // Assign a specific type
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60L * 60L))
                .user(user)
                .build();
        tokenRepository.save(resetToken);

        Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("userName", user.getFullName());
        templateProperties.put("resetUrl", normalizeBaseUrl(frontendBaseUrl) + "/reset-password?token=" + resetTokenValue);
        templateProperties.put("expiresHours", 1);
        templateProperties.put("requestIp", "N/A");
        templateProperties.put("requestDevice", null);
        templateProperties.put("locale", messageService.resolveLocaleForUser(user));

        emailTemplateService.sendTemplateEmail(
                user.getEmail(),
                EmailTemplate.PASSWORD_RESET.templateKey(),
                templateProperties,
                EmailSendMetadata.of(EmailCategory.SECURITY, EmailTemplate.PASSWORD_RESET.templateKey())
        );
        System.out.println("Password reset email triggered for: " + user.getEmail());
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        Token savedToken = tokenRepository.findTopByTokenOrderByIdDesc(token)
                .orElseThrow(() -> new RuntimeException(messageService.get(
                        "auth.passwordReset.invalidOrExpired",
                        "Invalid or expired password reset token."
                )));

        // Validate token type (ensure it's a password reset token)
        if (savedToken.getTokenType() != TokenType.PASSWORD_RESET) {
            throw new RuntimeException(messageService.get("auth.passwordReset.invalidTokenType", "Invalid token type for password reset."));
        }

        // Check if the token has expired
        if (Instant.now().isAfter(savedToken.getExpiresAt())) {
            // Optionally, you could resend a new reset email here or prompt the user to request a new one
            throw new RuntimeException(messageService.get("auth.passwordReset.expired", "Password reset token has expired. Please request a new one."));
        }

        // Check if the token has already been validated, expired or revoked
        if (savedToken.getValidatedAt() != null || savedToken.isExpired() || savedToken.isRevoked()) {
            throw new RuntimeException(messageService.get(
                    "auth.passwordReset.usedOrInvalid",
                    "Password reset token has already been used or is invalid."
            ));
        }

        UserEntity user = userRepository.findById(savedToken.getUser().getUuid())
                .orElseThrow(() -> new UsernameNotFoundException("Reset password: User not found for this token."));

        // Hash the new password and save it
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setTemporaryPasswordIssuedAt(null);
        user.setLastPasswordChangedAt(Instant.now());
        userRepository.save(user);
        revokeAllActiveUserTokens(user);

        // Invalidate the reset token after use
        savedToken.setValidatedAt(Instant.now());
        savedToken.setExpired(true);
        savedToken.setRevoked(true);
        savedToken.setExpiredAt(Instant.now());
        savedToken.setRevokedAt(Instant.now());
        tokenRepository.save(savedToken);

        System.out.println("Password reset successfully for user: " + user.getEmail());
    }

    @Override
    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean exists(UUID userId) {
        return userRepository.existsById(userId);
    }

    // method to build and save refresh token into the database
    public void saveUserToken(String theToken, UserEntity user, TokenType tokenType, String sessionId, String jwtId){
        Instant expiry = null;
        try {
            expiry = jwtUtil.extractExpiration(theToken).toInstant();
        } catch (Exception ignored) {
        }

        var token = Token.builder()
                .token(theToken)
                .jwtId(jwtId)
                .sessionId(sessionId)
                .user(user)
                .tokenType(tokenType)
                .createdAt(Instant.now())
                .expiresAt(expiry)
                .expired(false)
                .revoked(false)
                .build();

        tokenRepository.save(token);
    }

    private void revokeActiveSessionBearerTokens(UUID userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        tokenRepository.revokeAllActiveTokensByUserUuidAndSessionIdAndType(
                userId,
                sessionId,
                TokenType.BEARER,
                Instant.now()
        );
    }

    // revoking the currently existing bearer tokens
    public void revokeAllValidUserTokens(UserEntity user){
        List<Token> tokens = tokenRepository.findAllValidUserTokens(user.getUuid());
        if (tokens.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        tokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
            token.setExpiredAt(now);
            token.setRevokedAt(now);
        });
        tokenRepository.saveAll(tokens);
    }

    public void revokeAllActiveUserTokens(UserEntity user) {
        tokenRepository.revokeAllActiveTokensByUserUuid(user.getUuid(), Instant.now());
    }

    private AuthSessionResponse buildAuthSessionResponse(UserEntity user, boolean showWelcomeMessage) {
        Optional<TenantAdminAssignmentEntity> activeTenantAdminAssignment = resolveActiveTenantAdminAssignment(user);
        Set<String> roleNames = resolveSessionRoles(user, activeTenantAdminAssignment);
        Set<String> permissionKeys = resolveSessionPermissions(user, activeTenantAdminAssignment);

        Long churchId = null;
        String churchNumber = null;
        if (user.getTenant() != null && user.getTenant().getChurch() != null) {
            churchId = user.getTenant().getChurch().getChurchId();
            churchNumber = user.getTenant().getChurch().getChurchNumber();
        }

        String membershipStatus = null;
        if (user.getMembership() != null) {
            membershipStatus = user.getMembership().getStatus();
        }

        String priestNumber = null;
        if (UserType.PRIEST.equals(user.getUserType())) {
            priestNumber = user.getPriestNumber();
        }

        Long staffId = null;
        String staffNumber = null;
        StaffEntity staffProfile = user.getStaffProfile();
        if (staffProfile != null) {
            staffId = staffProfile.getId();
            staffNumber = staffProfile.getStaffNumber();
        }

        return AuthSessionResponse.builder()
                .userId(user.getUuid())
                .email(user.getEmail())
                .username(user.getFullName())
                .fullName(user.getFullName())
                .tenantId(user.getTenantId())
                .churchId(churchId)
                .churchNumber(churchNumber)
                .roles(roleNames)
                .permissions(permissionKeys)
                .membershipId(user.getMembershipId())
                .membershipStatus(membershipStatus)
                .staffId(staffId)
                .staffNumber(staffNumber)
                .priestNumber(priestNumber)
                .mustChangePassword(user.isMustChangePassword())
                .showWelcomeMessage(showWelcomeMessage)
                .build();
    }

    private Set<String> resolveSessionRoles(UserEntity user, Optional<TenantAdminAssignmentEntity> activeTenantAdminAssignment) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        activeTenantAdminAssignment
                .map(TenantAdminAssignmentEntity::getRole)
                .filter(TenantRole.PRIMARY_ADMIN::equals)
                .ifPresent(role -> {
                    roleNames.remove("ADMIN");
                    roleNames.add("PRIMARY_ADMIN");
                });

        return roleNames;
    }

    private Set<String> resolveSessionPermissions(UserEntity user, Optional<TenantAdminAssignmentEntity> activeTenantAdminAssignment) {
        Set<String> permissionKeys = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> {
                    if (permission == null || permission.getName() == null) {
                        return null;
                    }
                    return permission.getName().getName();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        activeTenantAdminAssignment
                .map(TenantAdminAssignmentEntity::getRole)
                .ifPresent(role -> permissionKeys.addAll(resolveTenantAdminPermissionKeys(role)));

        memberEffectivePermissionService.resolvePermissionTypes(user).stream()
                .map(PermissionType::getName)
                .forEach(permissionKeys::add);

        return permissionKeys;
    }

    private Optional<TenantAdminAssignmentEntity> resolveActiveTenantAdminAssignment(UserEntity user) {
        if (user.getTenantId() == null || user.getUuid() == null) {
            return Optional.empty();
        }

        return tenantAdminAssignmentRepository.findByTenant_IdAndUserId(user.getTenantId(), user.getUuid())
                .filter(assignment -> assignment.getStatus() == MembershipStatus.ACTIVE);
    }

    private Set<Role> resolveEffectiveRoles(UserEntity user) {
        Set<Role> roles = new LinkedHashSet<>(user.getRoles() == null ? Set.of() : user.getRoles());
        Optional<TenantAdminAssignmentEntity> activeAssignment = resolveActiveTenantAdminAssignment(user);
        if (activeAssignment.isEmpty()) {
            return roles;
        }

        TenantRole tenantRole = activeAssignment.get().getRole();
        if (tenantRole == TenantRole.PRIMARY_ADMIN) {
            roles.removeIf(role -> "ADMIN".equals(role.getRoleName()));
        }

        resolveRoleEntity(tenantRole).ifPresent(roles::add);
        return roles;
    }

    private Optional<Role> resolveRoleEntity(TenantRole tenantRole) {
        if (tenantRole == null) {
            return Optional.empty();
        }

        String roleName = switch (tenantRole) {
            case PRIMARY_ADMIN -> "PRIMARY_ADMIN";
            case ADMIN -> "ADMIN";
            case PRIMARY_OWNER, OWNER -> "OWNER";
            case FINANCE, COMMITTEE -> null;
        };

        return roleName == null ? Optional.empty() : roleRepository.findByRoleName(roleName);
    }

    private Set<String> resolveTenantAdminPermissionKeys(TenantRole tenantRole) {
        if (tenantRole == null) {
            return Set.of();
        }

        RoleType effectiveRoleType = switch (tenantRole) {
            case PRIMARY_OWNER, OWNER -> RoleType.OWNER;
            case PRIMARY_ADMIN -> RoleType.PRIMARY_ADMIN;
            case ADMIN -> RoleType.ADMIN;
            case FINANCE, COMMITTEE -> null;
        };

        if (effectiveRoleType == null) {
            return Set.of();
        }

        return effectiveRoleType.getPermissions().stream()
                .map(PermissionType::getName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isTwoFactorRequired(UserEntity user) {
        Optional<UserProfileEntity> maybeProfile = userProfileRepository.findById(user.getUuid());
        if (maybeProfile.isEmpty()) {
            return false;
        }
        UserProfileEntity profile = maybeProfile.get();
        return profile.isTwoFactorEnabled()
                && profile.getTotpSecretBase32() != null
                && !profile.getTotpSecretBase32().isBlank();
    }

    private AuthenticationResponse createTwoFactorChallenge(UserEntity user) {
        loginTwoFactorChallengeRepository.deleteByUserId(user.getUuid());
        loginTwoFactorChallengeRepository.deleteExpiredOrConsumed(Instant.now());

        String challengeToken = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        LoginTwoFactorChallengeEntity challenge = LoginTwoFactorChallengeEntity.builder()
                .challengeToken(challengeToken)
                .user(user)
                .attemptCount(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(LOGIN_2FA_CHALLENGE_MINUTES * 60L))
                .build();
        loginTwoFactorChallengeRepository.save(challenge);

        return AuthenticationResponse.builder()
                .challengeRequired(true)
                .challengeToken(challengeToken)
                .challengeType("TOTP_OR_BACKUP_CODE")
                .message("Two-factor verification required.")
                .build();
    }

    private boolean verifyTwoFactorInput(UserProfileEntity profile, UserEntity user, String input) {
        String secret = profile.getTotpSecretBase32();
        if (secret != null && input.matches("\\d{6}")) {
            boolean validTotp = TotpUtils.verifyTotpCode(secret, input, Instant.now(), 1);
            if (validTotp) {
                return true;
            }
        }

        String normalizedBackupInput = input.toUpperCase(Locale.ROOT).replace(" ", "");
        if (normalizedBackupInput.length() == 8 && !normalizedBackupInput.contains("-")) {
            normalizedBackupInput = normalizedBackupInput.substring(0, 4) + "-" + normalizedBackupInput.substring(4);
        }

        List<UserTwoFactorBackupCodeEntity> activeBackupCodes = backupCodeRepository.findActiveByUserId(user.getUuid());

        for (UserTwoFactorBackupCodeEntity backupCode : activeBackupCodes) {
            if (passwordEncoder.matches(normalizedBackupInput, backupCode.getCodeHash())) {
                backupCode.setUsedAt(Instant.now());
                backupCodeRepository.save(backupCode);
                return true;
            }
        }
        return false;
    }

    public void sendValidationEmail(UserEntity user) throws MessagingException {
        // Generate and save the activation token
        var newToken = generateAndSaveActivationToken(user);

        // One-click activation URL (frontend will verify using the token)
        String activationUrl = normalizeBaseUrl(frontendBaseUrl) + "/auth/activate?token="
                + java.net.URLEncoder.encode(newToken, java.nio.charset.StandardCharsets.UTF_8)
                + "&email="
                + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8);

        // Prepare the properties map for the email template
        Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("userName", user.getFullName());
        templateProperties.put("verifyUrl", activationUrl);
        templateProperties.put("expiresHours", 24);
        templateProperties.put("locale", messageService.resolveLocaleForUser(user));

        emailTemplateService.sendTemplateEmail(
                user.getEmail(),
                EmailTemplate.VERIFY_EMAIL_LINK.templateKey(),
                templateProperties,
                EmailSendMetadata.of(EmailCategory.SECURITY, EmailTemplate.VERIFY_EMAIL_LINK.templateKey())
        );

        System.out.println("Validation email triggered for: " + user.getEmail());
    }

    private String generateAndSaveActivationToken(UserEntity user) {
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .tokenType(TokenType.ACTIVATION)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(15 * 60L))
                .user(user)
                .build();

        tokenRepository.save(token);

        return generatedToken;
    }

    private String normalizeBaseUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalStateException("app.public.frontend-base-url must be configured");
        }
        return rawUrl.endsWith("/") ? rawUrl.substring(0, rawUrl.length() - 1) : rawUrl;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException(messageService.get("auth.activation.emailRequired", "Email is required for activation"));
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int randomIndex = SECURE_RANDOM.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }


}
