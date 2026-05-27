package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
    private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final long PASSWORD_RESET_TOKEN_TTL_SECONDS = 60L * 60L;

    private final TokenRepository tokenRepository;

    public IssuedPasswordResetToken issueForUser(UserEntity user) {
        String rawToken = generatePasswordResetToken();
        String tokenHash = hashToken(rawToken);
        Instant now = Instant.now();

        List<Token> existingResetTokens = tokenRepository.findAllValidTokensByUser(user.getUuid(), TokenType.PASSWORD_RESET);
        existingResetTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
            token.setExpiredAt(now);
            token.setRevokedAt(now);
        });
        if (!existingResetTokens.isEmpty()) {
            tokenRepository.saveAll(existingResetTokens);
        }

        Token resetToken = Token.builder()
                .token(tokenHash)
                .tokenType(TokenType.PASSWORD_RESET)
                .createdAt(now)
                .expiresAt(now.plusSeconds(PASSWORD_RESET_TOKEN_TTL_SECONDS))
                .user(user)
                .build();
        Token savedToken = tokenRepository.save(resetToken);
        return new IssuedPasswordResetToken(savedToken.getId(), rawToken, savedToken.getExpiresAt());
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    private String generatePasswordResetToken() {
        byte[] bytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
