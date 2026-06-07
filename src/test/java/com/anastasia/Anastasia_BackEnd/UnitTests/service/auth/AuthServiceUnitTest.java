package com.anastasia.Anastasia_BackEnd.UnitTests.service.auth;


import com.anastasia.Anastasia_BackEnd.common.cache.CacheWarmupService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationResponse;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.AuthenticationProcessException;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.InvalidCredentialsException;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.model.LoginTwoFactorChallengeEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.LoginTwoFactorChallengeRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.core.auth.service.IssuedPasswordResetToken;
import com.anastasia.Anastasia_BackEnd.core.auth.service.PasswordResetTokenService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.MemberEffectivePermissionService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.TokenHashingService;
import com.anastasia.Anastasia_BackEnd.core.auth.support.ActivationTokenObserver;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserPreferencesEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserProfileEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthServiceImpl;
import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailTemplateService;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserPreferencesRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserTwoFactorBackupCodeRepository;
import com.anastasia.Anastasia_BackEnd.common.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@LenientMockitoTest
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthServiceUnitTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenCookieService refreshTokenCookieService;
    @Mock private TokenHashingService tokenHashingService;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private LocalizedMessageService messageService;
    @Mock private CacheWarmupService cacheWarmupService;
    @Mock private RoleRepository roleRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserPreferencesRepository userPreferencesRepository;
    @Mock private UserTwoFactorBackupCodeRepository backupCodeRepository;
    @Mock private LoginTwoFactorChallengeRepository loginTwoFactorChallengeRepository;
    @Mock private TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private MemberEffectivePermissionService memberEffectivePermissionService;
    @Mock private ActivationTokenObserver activationTokenObserver;

    private AuthServiceImpl authService;

    private UserEntity user;
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        authService = spy(new AuthServiceImpl(
                jwtUtil,
                refreshTokenCookieService,
                tokenHashingService,
                userRepository,
                passwordEncoder,
                authenticationManager,
                tokenRepository,
                passwordResetTokenService,
                emailTemplateService,
                messageService,
                cacheWarmupService,
                roleRepository,
                userProfileRepository,
                userPreferencesRepository,
                backupCodeRepository,
                loginTwoFactorChallengeRepository,
                tenantAdminAssignmentRepository,
                tenantRepository,
                staffRepository,
                memberEffectivePermissionService,
                Optional.of(activationTokenObserver)
        ));
        user = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email(email)
                .password("plain")
                .fullName("Test User")
                .userType(UserType.GUEST)
                .status(UserStatus.PENDING_VERIFICATION)
                .createdAt(Instant.now().minusSeconds(24L * 60L * 60L))
                .build();
        lenient().when(messageService.get(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.get(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(messageService.resolveLocaleForUser(any(UserEntity.class))).thenReturn(Locale.ENGLISH);
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    void testCreateUser_ShouldThrowIfEmailExists() {
        when(userRepository.existsByEmail(email)).thenReturn(true);
        assertThrows(Exception.class, () -> authService.createUser(user));
    }

    @Test
    void testFindUserByEmail_ShouldReturnUser() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        assertTrue(authService.findUserByEmail(email).isPresent());
    }

    @Test
    void testFindUserByEmail_ShouldReturnEmpty() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertFalse(authService.findUserByEmail(email).isPresent());
    }

    @Test
    void testResendActivationEmail_ShouldThrowIfNotFound() {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.resendActivationEmail(email));
    }

    @Test
    void testResendActivationEmail_ShouldThrowIfAlreadyVerified() {
        user.setVerified(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        assertThrows(IllegalStateException.class, () -> authService.resendActivationEmail(email));
    }

    @Test
    void initiatePasswordReset_shouldReturnSilentlyWhenUserMissing() throws Exception {
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        authService.initiatePasswordReset(email);

        verify(tokenRepository, never()).save(any());
        verify(emailTemplateService, never()).sendTemplateEmail(any(), any(), any(), any());
    }

    @Test
    void initiatePasswordReset_shouldStoreHashedTokenAndEmailRawToken() throws Exception {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetTokenService.issueForUser(user)).thenReturn(
                new IssuedPasswordResetToken(42, "raw-reset-token-value-with-sufficient-length", Instant.now().plusSeconds(3600))
        );

        authService.initiatePasswordReset(email);

        verify(passwordResetTokenService).issueForUser(user);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailTemplateService).sendTemplateEmail(eq(email), any(), propertiesCaptor.capture(), any());

        String resetUrl = propertiesCaptor.getValue().get("resetUrl").toString();
        String rawToken = resetUrl.substring(resetUrl.indexOf("token=") + "token=".length());

        verify(tokenRepository, never()).save(any(Token.class));
        assertEquals("raw-reset-token-value-with-sufficient-length", rawToken);
        assertTrue(rawToken.length() >= 40);
    }

    @Test
    void testExists_ShouldReturnTrue() {
        when(userRepository.existsById(user.getUuid())).thenReturn(true);
        assertTrue(authService.exists(user.getUuid()));
    }

    @Test
    void activateAccount_shouldRequireTokenForProvidedEmail() {
        when(tokenHashingService.hashToken("123456")).thenReturn("hashed-123456");
        UserEntity otherUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("other@example.com")
                .build();
        Token token = Token.builder()
                .token("hashed-123456")
                .tokenType(TokenType.ACTIVATION)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .user(otherUser)
                .build();
        when(tokenRepository.findActiveTokensByValueAndType("hashed-123456", TokenType.ACTIVATION)).thenReturn(List.of(token));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.activateAccount("123456", email));

        assertEquals("Invalid activation token", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void activateAccount_shouldRejectAndMarkExpiredToken() {
        when(tokenHashingService.hashToken("123456")).thenReturn("hashed-123456");
        Token token = Token.builder()
                .token("hashed-123456")
                .tokenType(TokenType.ACTIVATION)
                .createdAt(Instant.now().minusSeconds(600))
                .expiresAt(Instant.now().minusSeconds(1))
                .user(user)
                .build();
        when(tokenRepository.findActiveTokensByValueAndType("hashed-123456", TokenType.ACTIVATION)).thenReturn(List.of(token));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.activateAccount("123456", email));

        assertEquals("Activation token has expired. Please request a new activation email.", ex.getMessage());
        assertTrue(token.isExpired());
        assertNotNull(token.getExpiredAt());
        verify(tokenRepository).save(token);
        verify(userRepository, never()).save(any());
    }

    @Test
    void activateAccount_shouldActivateUserAndBurnToken() {
        when(tokenHashingService.hashToken("123456")).thenReturn("hashed-123456");
        Token token = Token.builder()
                .token("hashed-123456")
                .tokenType(TokenType.ACTIVATION)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .user(user)
                .build();
        when(tokenRepository.findActiveTokensByValueAndType("hashed-123456", TokenType.ACTIVATION)).thenReturn(List.of(token));
        when(userRepository.findById(user.getUuid())).thenReturn(Optional.of(user));
        doReturn(AuthenticationResponse.builder().accessToken("access").build())
                .when(authService).issueSessionForUser(user.getUuid());

        AuthenticationResponse response = authService.activateAccount("123456", " Test@Example.com ");

        assertEquals("access", response.getAccessToken());
        assertTrue(user.isVerified());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(token.isExpired());
        assertNotNull(token.getValidatedAt());
        assertNotNull(token.getExpiredAt());
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void sendValidationEmail_shouldStoreHashedActivationTokenAndEmailRawToken() throws Exception {
        when(tokenHashingService.hashToken(anyString())).thenReturn("hashed-activation-token");

        authService.sendValidationEmail(user);

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertEquals("hashed-activation-token", tokenCaptor.getValue().getToken());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailTemplateService).sendTemplateEmail(eq(email), any(), propertiesCaptor.capture(), any());

        String verifyUrl = propertiesCaptor.getValue().get("verifyUrl").toString();
        String rawToken = verifyUrl.substring(
                verifyUrl.indexOf("token=") + "token=".length(),
                verifyUrl.indexOf("&email=")
        );

        assertTrue(rawToken.length() >= 40);
        assertFalse(rawToken.matches("\\d{6}"));
    }

    @Test
    void testAuthenticate_ShouldThrowInvalidCredentialsForBadCredentials() {
        AuthenticationRequest req = new AuthenticationRequest(email, "badpass");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(req));
    }

    @Test
    void testAuthenticate_ShouldPropagateAuthenticationExceptions() {
        AuthenticationRequest req = new AuthenticationRequest(email, "badpass");
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        DisabledException ex = assertThrows(DisabledException.class, () -> authService.authenticate(req));

        assertEquals("User is disabled", ex.getMessage());
    }

    @Test
    void testAuthenticate_ShouldWrapUnexpectedErrors() {
        AuthenticationRequest req = new AuthenticationRequest(email, "badpass");
        RuntimeException cause = new RuntimeException("Fail");
        when(authenticationManager.authenticate(any())).thenThrow(cause);

        AuthenticationProcessException ex = assertThrows(AuthenticationProcessException.class, () -> authService.authenticate(req));

        assertEquals("An unexpected error occurred during login", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    void testAuthenticate_ShouldRejectFreshUnverifiedUserBeforeAuthentication() {
        AuthenticationRequest req = new AuthenticationRequest(email, "secret");
        user.setCreatedAt(Instant.now().minusSeconds(60));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.authenticate(req));

        assertEquals("Login: Account is not verified. Please find the token sent to you for verification!", ex.getMessage());
        verify(authenticationManager, never()).authenticate(any());
        verify(emailTemplateService, never()).sendTemplateEmail(any(), any(), any(), any());
    }

    @Test
    void testAuthenticate_ShouldResendVerificationForOlderUnverifiedUserBeforeAuthentication() {
        AuthenticationRequest req = new AuthenticationRequest(email, "secret");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.authenticate(req));

        assertEquals("Login: Account is not verified. Please find a new token sent to you for verification!", ex.getMessage());
        verify(authenticationManager, never()).authenticate(any());
        verify(tokenRepository).save(any(Token.class));
        verify(emailTemplateService).sendTemplateEmail(any(), any(), any(), any());
    }

    @Test
    void testSaveUserToken_ShouldCallRepo() {
        when(tokenHashingService.hashToken("refresh-token")).thenReturn("refresh-hash");

        authService.saveUserToken("abc123", user, TokenType.BEARER, "session-1", "jwt-1");
        authService.saveUserToken("refresh-token", user, TokenType.REFRESH, "session-1", "jwt-2");

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository, times(2)).save(tokenCaptor.capture());

        List<Token> savedTokens = tokenCaptor.getAllValues();
        assertNull(savedTokens.get(0).getToken());
        assertEquals("jwt-1", savedTokens.get(0).getJwtId());
        assertEquals("refresh-hash", savedTokens.get(1).getToken());
        assertEquals("jwt-2", savedTokens.get(1).getJwtId());
    }

    @Test
    void refreshToken_shouldLookupHashedRefreshTokenAndPersistNewBearerWithoutRawJwt() {
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        String refreshToken = "refresh.jwt";
        String refreshHash = "refresh-hash";
        String sessionId = "session-1";
        String refreshJwtId = "refresh-jti";
        String accessToken = "new-access.jwt";

        user.setVerified(true);
        when(refreshTokenCookieService.extractRefreshToken(request)).thenReturn(Optional.of(refreshToken));
        when(tokenHashingService.hashToken(refreshToken)).thenReturn(refreshHash);
        when(tokenRepository.findActiveTokensByValueAndType(refreshHash, TokenType.REFRESH))
                .thenReturn(List.of(Token.builder()
                        .token(refreshHash)
                        .tokenType(TokenType.REFRESH)
                        .sessionId(sessionId)
                        .jwtId(refreshJwtId)
                        .user(user)
                        .expired(false)
                        .revoked(false)
                        .build()));
        when(jwtUtil.extractUsername(refreshToken)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtUtil.extractJwtId(refreshToken)).thenReturn(refreshJwtId);
        when(jwtUtil.isTokenValid(eq(refreshToken), any())).thenReturn(true);
        when(memberEffectivePermissionService.resolvePermissions(user)).thenReturn(Set.of());
        when(jwtUtil.generateAccessToken(any(), eq(sessionId), anyString())).thenReturn(accessToken);
        when(jwtUtil.extractExpiration(accessToken)).thenReturn(java.util.Date.from(Instant.now().plusSeconds(3600)));

        authService.refreshToken(request);

        verify(tokenRepository).findActiveTokensByValueAndType(refreshHash, TokenType.REFRESH);
        verify(tokenRepository).revokeAllActiveTokensByUserUuidAndSessionIdAndType(
                eq(user.getUuid()),
                eq(sessionId),
                eq(TokenType.BEARER),
                any()
        );

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepository, atLeastOnce()).save(tokenCaptor.capture());
        Token savedBearer = tokenCaptor.getValue();
        assertEquals(TokenType.BEARER, savedBearer.getTokenType());
        assertNull(savedBearer.getToken());
    }

    @Test
    void testRevokeAllValidUserTokens_ShouldRevokeAndSave() {
        Token token = Token.builder().tokenType(TokenType.BEARER).revoked(false).expired(false).build();
        when(tokenRepository.findAllValidUserTokens(user.getUuid())).thenReturn(List.of(token));
        authService.revokeAllValidUserTokens(user);
        assertTrue(token.isRevoked());
        assertTrue(token.isExpired());
        verify(tokenRepository).saveAll(any());
    }

    @Test
    void testRevokeAllValidUserTokens_ShouldSkipIfEmpty() {
        when(tokenRepository.findAllValidUserTokens(user.getUuid())).thenReturn(List.of());
        authService.revokeAllValidUserTokens(user);
        verify(tokenRepository, never()).saveAll(any());
    }

    @Test
    void authenticateGoogleUser_ShouldCreateMissingProfileAndPreferencesForNewUser() {
        Role userRole = Role.builder().id(1L).roleName("USER").build();
        UserEntity savedUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("test@example.com")
                .fullName("Test User")
                .googleId("google-123")
                .status(UserStatus.ACTIVE)
                .emailVerifiedAt(Instant.now())
                .roles(java.util.Set.of(userRole))
                .build();

        when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);
        when(userProfileRepository.findById(savedUser.getUuid())).thenReturn(Optional.empty());
        when(userPreferencesRepository.findById(savedUser.getUuid())).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userPreferencesRepository.save(any(UserPreferencesEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doReturn(AuthenticationResponse.builder().accessToken("access").refreshToken("refresh").build())
                .when(authService).issueSessionForUser(savedUser.getUuid());

        AuthenticationResponse response = authService.authenticateGoogleUser("google-123", "Test@Example.com", "Test User");

        assertEquals("access", response.getAccessToken());
        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(userProfileRepository).save(any(UserProfileEntity.class));
        verify(userPreferencesRepository).save(any(UserPreferencesEntity.class));
    }

    @Test
    void authenticateGoogleUser_ShouldLinkExistingManualAccountByEmailIgnoringCase() {
        UserEntity existingUser = UserEntity.builder()
                .uuid(UUID.randomUUID())
                .email("ManualUser@Example.com")
                .fullName("Manual User")
                .status(UserStatus.PENDING_VERIFICATION)
                .roles(java.util.Set.of(Role.builder().id(1L).roleName("USER").build()))
                .build();

        when(userRepository.findByGoogleId("google-456")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("manualuser@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(userProfileRepository.findById(existingUser.getUuid())).thenReturn(Optional.of(UserProfileEntity.builder().user(existingUser).build()));
        when(userPreferencesRepository.findById(existingUser.getUuid())).thenReturn(Optional.of(UserPreferencesEntity.builder().user(existingUser).build()));

        doReturn(AuthenticationResponse.builder().accessToken("access").refreshToken("refresh").build())
                .when(authService).issueSessionForUser(existingUser.getUuid());

        authService.authenticateGoogleUser("google-456", "ManualUser@Example.com", "Manual User Updated");

        assertEquals("google-456", existingUser.getGoogleId());
        assertEquals("manualuser@example.com", existingUser.getEmail());
        assertTrue(existingUser.isVerified());
        assertEquals(UserStatus.ACTIVE, existingUser.getStatus());
        assertEquals("Manual User Updated", existingUser.getFullName());
        verify(userProfileRepository, never()).save(any(UserProfileEntity.class));
        verify(userPreferencesRepository, never()).save(any(UserPreferencesEntity.class));
    }
}
