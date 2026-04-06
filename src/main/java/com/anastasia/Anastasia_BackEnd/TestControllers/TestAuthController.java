package com.anastasia.Anastasia_BackEnd.TestControllers;

import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile({"test", "test-server", "api"})  // Only available in test profiles
@RestController
@RequestMapping("/api/v1/auth/test")
public class TestAuthController {

    private final TokenRepository tokenRepository;

    public TestAuthController(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    // Endpoint to retrieve activation token by email
    @GetMapping("/activation-token")
    public String getActivationToken(@RequestParam String email) {
        return tokenRepository.findAll()
                .stream()
                .filter(t -> t.getUser() != null &&
                        t.getUser().getEmail().equalsIgnoreCase(email) &&
                        !t.isExpired() && !t.isRevoked())
                .map(Token::getToken)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active activation token found for " + email));
    }

    @GetMapping("/refresh-token")
    public String getRefreshToken(@RequestParam String email) {
        return tokenRepository.findTopByUserEmailIgnoreCaseAndTokenTypeAndDeletedAtIsNullOrderByIdDesc(email, TokenType.REFRESH)
                .map(Token::getToken)
                .orElseThrow(() -> new RuntimeException("No refresh token found for " + email));
    }
}
