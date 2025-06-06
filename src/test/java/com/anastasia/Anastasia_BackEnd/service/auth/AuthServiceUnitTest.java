package com.anastasia.Anastasia_BackEnd.service.auth;


import com.anastasia.Anastasia_BackEnd.model.auth.AuthenticationRequest;
import com.anastasia.Anastasia_BackEnd.model.token.Token;
import com.anastasia.Anastasia_BackEnd.model.token.TokenType;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.TokenRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.email.EmailService;
import com.anastasia.Anastasia_BackEnd.util.JwtUtilTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
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
    @Mock private EmailService emailService;
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
                .verified(false)
                .createdDate(LocalDateTime.now().minusDays(1))
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
        authService.saveUserToken("abc123", user, TokenType.BEARER);
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
