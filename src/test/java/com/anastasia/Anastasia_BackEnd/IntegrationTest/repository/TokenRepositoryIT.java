package com.anastasia.Anastasia_BackEnd.IntegrationTest.repository;

import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.TestSupport.ServiceIntegrationTestBase;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Integration Tests")
@Feature("Repository Layer - TokenRepository")
class TokenRepositoryIT extends ServiceIntegrationTestBase {

    @Autowired private TokenRepository tokenRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void tokenRepository_handlesLifecycleOperations() {
        Role ownerRole = fetchRole(RoleType.OWNER);
        UserEntity user = persistUser("token+" + UUID.randomUUID() + "@integration.com", ownerRole);

        Token active = tokenRepository.save(Token.builder()
                .token(UUID.randomUUID().toString())
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .user(user)
                .build());

        Token expiring = tokenRepository.save(Token.builder()
                .token(UUID.randomUUID().toString())
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .expiryDate(Instant.now().minusSeconds(120))
                .user(user)
                .build());

        Token old = tokenRepository.save(Token.builder()
                .token(UUID.randomUUID().toString())
                .tokenType(TokenType.BEARER)
                .expired(true)
                .revoked(true)
                .user(user)
                .build());

        List<Token> validTokens = tokenRepository.findAllValidUserTokens(user.getUuid());
        assertThat(validTokens).extracting(Token::getId).contains(active.getId(), expiring.getId());

        expiring.setExpired(true);
        tokenRepository.save(expiring);

        tokenRepository.deleteExpiredAndRevokedTokens();
        assertThat(tokenRepository.findById(expiring.getId())).isEmpty();
        assertThat(tokenRepository.findById(old.getId())).isEmpty();
        assertThat(tokenRepository.findById(active.getId())).isPresent();

        List<Token> bearerTokens = tokenRepository.findAllValidTokensByUser(user.getUuid(), TokenType.BEARER);
        assertThat(bearerTokens).extracting(Token::getId).containsExactly(active.getId());
    }
}
