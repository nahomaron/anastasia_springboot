package com.anastasia.Anastasia_BackEnd.UnitTests.service.auth;


import com.anastasia.Anastasia_BackEnd.core.auth.dto.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthServiceImpl;
import com.anastasia.Anastasia_BackEnd.util.JwtUtilTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceUnitTest {

    @Mock private JwtUtilTest jwtUtil;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenRepository tokenRepository;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private RoleRepository roleRepository;

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
                .verified(false)
                .createdAt(Instant.now().minusSeconds(24L * 60L * 60L))
                .build();
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
    void testAuthenticate_ShouldThrowIfAuthFails() {
        AuthenticationRequest req = new AuthenticationRequest(email, "badpass");
        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Fail"));
        assertThrows(RuntimeException.class, () -> authService.authenticate(req));
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
