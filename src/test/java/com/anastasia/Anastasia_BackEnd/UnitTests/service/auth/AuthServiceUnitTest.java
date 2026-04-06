package com.anastasia.Anastasia_BackEnd.UnitTests.service.auth;


import com.anastasia.Anastasia_BackEnd.common.cache.CacheWarmupService;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.AuthenticationProcessException;
import com.anastasia.Anastasia_BackEnd.common.exception.customExceptions.InvalidCredentialsException;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.model.LoginTwoFactorChallengeEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.LoginTwoFactorChallengeRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.core.auth.service.RefreshTokenCookieService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
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
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserProfileRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserTwoFactorBackupCodeRepository;
import com.anastasia.Anastasia_BackEnd.util.JwtUtilTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@LenientMockitoTest
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthServiceUnitTest {

    @Mock private JwtUtilTest jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenCookieService refreshTokenCookieService;
    @Mock private TokenRepository tokenRepository;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private LocalizedMessageService messageService;
    @Mock private CacheWarmupService cacheWarmupService;
    @Mock private RoleRepository roleRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserTwoFactorBackupCodeRepository backupCodeRepository;
    @Mock private LoginTwoFactorChallengeRepository loginTwoFactorChallengeRepository;
    @Mock private TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    @Mock private StaffRepository staffRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserEntity user;
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
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
    void testExists_ShouldReturnTrue() {
        when(userRepository.existsById(user.getUuid())).thenReturn(true);
        assertTrue(authService.exists(user.getUuid()));
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
        authService.saveUserToken("abc123", user, TokenType.BEARER, "session-1", "jwt-1");
        verify(tokenRepository).save(any(Token.class));
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
}
