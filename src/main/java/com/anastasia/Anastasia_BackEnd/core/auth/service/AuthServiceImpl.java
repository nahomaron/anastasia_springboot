package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.AuthenticationProcessException;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.InvalidCredentialsException;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthSessionResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.VerifyLoginTwoFactorRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.model.LoginTwoFactorChallengeEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.LoginTwoFactorChallengeRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.common.cache.CacheWarmupService;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateName;
import com.anastasia.Anastasia_BackEnd.core.auth.permission.Permission;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserTwoFactorBackupCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserTwoFactorBackupCodeRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.security.TotpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.IllegalWriteException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final CacheWarmupService cacheWarmupService;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserTwoFactorBackupCodeRepository backupCodeRepository;
    private final LoginTwoFactorChallengeRepository loginTwoFactorChallengeRepository;

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
            throw new RuntimeException("User creation failed: " + e.getMessage());
        }

    }

    @Override
    public AuthenticationResponse activateAccount(String token) {
        Token savedToken = tokenRepository.findTopByTokenOrderByIdDesc(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (savedToken.getTokenType() != TokenType.ACTIVATION) {
            throw new RuntimeException("Invalid activation token");
        }

//        if(LocalDateTime.now().isAfter(savedToken.getExpiresAt())){
//            sendValidationEmail(savedToken.getUser());
//            throw new RuntimeException("Activation token has expired. Please find the new token sent to you!");
//        }

        var user = userRepository.findById(savedToken.getUser().getUuid())
                .orElseThrow(() -> new UsernameNotFoundException("Activation - Username not found"));

        if (!user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }
        if (savedToken.getValidatedAt() == null) {
            savedToken.setValidatedAt(LocalDateTime.now());
            tokenRepository.save(savedToken);
        }

        return issueSessionForUser(user.getUuid());
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws MessagingException {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        } catch (Exception e) {
            throw new AuthenticationProcessException("An unexpected error occurred during login");
        }


        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Login - Username not found"));


        if(!user.isVerified()){
            if (user.getCreatedDate().isBefore(LocalDateTime.now().minusHours(24))) {
                // The user was created more than 24 hours ago
                sendValidationEmail(user);
                throw new RuntimeException("Login: Account is not verified. Please find a new token sent to you for verification!");
            }
            throw new RuntimeException("Login: Account is not verified. Please find the token sent to you for verification!");
        }

        if (isTwoFactorRequired(user)) {
            return createTwoFactorChallenge(user);
        }

        return issueSessionForUser(user.getUuid());
    }

    @Override
    public AuthenticationResponse verifyLoginTwoFactor(VerifyLoginTwoFactorRequest request) {
        String challengeToken = request.getChallengeToken().trim();
        LoginTwoFactorChallengeEntity challenge = loginTwoFactorChallengeRepository.findByChallengeToken(challengeToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid two-factor challenge."));

        if (challenge.getConsumedAt() != null) {
            throw new IllegalArgumentException("Two-factor challenge already used.");
        }
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Two-factor challenge expired. Please login again.");
        }
        if (challenge.getAttemptCount() >= LOGIN_2FA_MAX_ATTEMPTS) {
            throw new IllegalStateException("Too many invalid two-factor attempts. Please login again.");
        }

        UserEntity user = challenge.getUser();
        UserProfileEntity profile = userProfileRepository.findById(user.getUuid())
                .orElseThrow(() -> new IllegalStateException("Two-factor profile is missing."));

        String input = request.getCode() == null ? "" : request.getCode().trim();
        boolean valid = verifyTwoFactorInput(profile, user, input);

        challenge.setAttemptCount(challenge.getAttemptCount() + 1);
        if (!valid) {
            loginTwoFactorChallengeRepository.save(challenge);
            throw new IllegalArgumentException("Invalid verification code.");
        }

        challenge.setConsumedAt(LocalDateTime.now());
        loginTwoFactorChallengeRepository.save(challenge);
        return issueSessionForUser(user.getUuid());
    }

    @Override
    public AuthenticationResponse issueSessionForUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Session issue failed - user not found"));

        if (user.isAccountLocked()) {
            throw new IllegalStateException("User account is locked.");
        }
        if (!user.isVerified()) {
            throw new IllegalStateException("User account is not verified.");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);
        AuthSessionResponse session = buildAuthSessionResponse(user);

        var jwtToken = jwtUtil.generateAccessToken(userPrincipal);
        var refreshToken = jwtUtil.generateRefreshToken(userPrincipal);

        revokeAllValidUserTokens(user);
        saveUserToken(jwtToken, user, TokenType.BEARER);
        saveUserToken(refreshToken, user, TokenType.REFRESH);

        if (userPrincipal.getRoles().stream().anyMatch(role -> role.getRoleName().equals("ADMIN"))
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

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        final String authHeader = request.getHeader("Authorization");
        String refreshToken = null;
        String username = null;

        if(authHeader != null && authHeader.startsWith("Bearer ")){
            refreshToken = authHeader.substring(7);
            username = jwtUtil.extractUsername(refreshToken);
        }

        if(username != null){
            UserEntity user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid, User doesn't exist"));

            UserPrincipal userPrincipal = new UserPrincipal(user);

            if(jwtUtil.isTokenValid(refreshToken, userPrincipal)){
                var accessToken = jwtUtil.generateAccessToken(userPrincipal);
                revokeAllValidUserTokens(user);
                saveUserToken(accessToken, user, TokenType.BEARER);

                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

                try {
                    new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
            throw new IllegalStateException("User is already verified");
        }

        // Generate and send a new activation email
        sendValidationEmail(user);
    }

    public void initiatePasswordReset(String email) throws MessagingException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String resetTokenValue = generateActivationCode(8); // Longer token for security
        // Invalidate any existing password reset tokens for this user first (optional but good practice)
        tokenRepository.findAllValidTokensByUser(user.getUuid(), TokenType.PASSWORD_RESET) // Assuming you add TokenType.PASSWORD_RESET
                .forEach(token -> {
                    token.setExpired(true);
                    token.setRevoked(true);
                });

        Token resetToken = Token.builder()
                .token(resetTokenValue)
                .tokenType(TokenType.PASSWORD_RESET) // Assign a specific type
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(1)) // Reset token valid for 1 hour
                .user(user)
                .build();
        tokenRepository.save(resetToken);

        Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("username", user.getFullName());
        templateProperties.put("reset_url", "http://localhost:3000/reset-password?token=" + resetTokenValue);
        templateProperties.put("reset_token", resetTokenValue); // Also provide the raw token if needed

        emailNotificationService.sendEmail(
                user.getEmail(),
                "Password Reset for Anastasia Account",
                EmailTemplateName.RESET_PASSWORD, // Make sure you have a reset_password.html template
                templateProperties
        );
        System.out.println("Password reset email triggered for: " + user.getEmail());
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        Token savedToken = tokenRepository.findTopByTokenOrderByIdDesc(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired password reset token."));

        // Validate token type (ensure it's a password reset token)
        if (savedToken.getTokenType() != TokenType.PASSWORD_RESET) {
            throw new RuntimeException("Invalid token type for password reset.");
        }

        // Check if the token has expired
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            // Optionally, you could resend a new reset email here or prompt the user to request a new one
            throw new RuntimeException("Password reset token has expired. Please request a new one.");
        }

        // Check if the token has already been validated, expired or revoked
        if (savedToken.getValidatedAt() != null || savedToken.isExpired() || savedToken.isRevoked()) {
            throw new RuntimeException("Password reset token has already been used or is invalid.");
        }

        UserEntity user = userRepository.findById(savedToken.getUser().getUuid())
                .orElseThrow(() -> new UsernameNotFoundException("Reset password: User not found for this token."));

        // Hash the new password and save it
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate the reset token after use
        savedToken.setValidatedAt(LocalDateTime.now());
        savedToken.setExpired(true);
        savedToken.setRevoked(true);
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
    public void saveUserToken(String theToken, UserEntity user, TokenType tokenType){
        Instant expiry = null;
        try {
            expiry = jwtUtil.extractExpiration(theToken).toInstant();
        } catch (Exception ignored) {
        }

        var token = Token.builder()
                .token(theToken)
                .user(user)
                .tokenType(tokenType)
                .createdAt(LocalDateTime.now())
                .expiryDate(expiry)
                .expired(false)
                .revoked(false)
                .build();

        tokenRepository.save(token);
    }

    // revoking the currently existing refreshTokens
    public void revokeAllValidUserTokens(UserEntity user){
        var validUserTokens = tokenRepository.findAllValidUserTokens(user.getUuid());

        if (validUserTokens.isEmpty()) return;

        validUserTokens.forEach(token -> {
            if(token.getTokenType() == TokenType.BEARER){
                token.setRevoked(true);
                token.setExpired(true);
            }
        });
        tokenRepository.saveAll(validUserTokens);
    }

    private AuthSessionResponse buildAuthSessionResponse(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

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

        return AuthSessionResponse.builder()
                .userId(user.getUuid())
                .email(user.getEmail())
                .username(user.getFullName())
                .tenantId(user.getTenantId())
                .churchId(churchId)
                .churchNumber(churchNumber)
                .roles(roleNames)
                .permissions(permissionKeys)
                .membershipId(user.getMembershipId())
                .membershipStatus(membershipStatus)
                .priestNumber(priestNumber)
                .build();
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
        loginTwoFactorChallengeRepository.deleteExpiredOrConsumed(LocalDateTime.now());

        String challengeToken = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        LoginTwoFactorChallengeEntity challenge = LoginTwoFactorChallengeEntity.builder()
                .challengeToken(challengeToken)
                .user(user)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(LOGIN_2FA_CHALLENGE_MINUTES))
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
                backupCode.setUsedAt(LocalDateTime.now());
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
        String activationUrl = "http://localhost:4200/auth/activate?token="
                + java.net.URLEncoder.encode(newToken, java.nio.charset.StandardCharsets.UTF_8)
                + "&email="
                + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8);

        // Prepare the properties map for the email template
        Map<String, Object> templateProperties = new HashMap<>();
        templateProperties.put("username", user.getFullName());
        templateProperties.put("confirmation_url", activationUrl);
        templateProperties.put("activation_code", newToken);

        // Call the enhanced EmailService with the dynamic properties map
        emailNotificationService.sendEmail(
                user.getEmail(),
                "Account Activation for Anastasia",
                EmailTemplateName.ACTIVATE_ACCOUNT,
                templateProperties
        );

        System.out.println("Validation email triggered for: " + user.getEmail());
    }

    private String generateAndSaveActivationToken(UserEntity user) {
        String generatedToken = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .tokenType(TokenType.ACTIVATION)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();

        tokenRepository.save(token);

        return generatedToken;
    }

    private String generateActivationCode(int length) {
        String characters = "01234456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }


}
