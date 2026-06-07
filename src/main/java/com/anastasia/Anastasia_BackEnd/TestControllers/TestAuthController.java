package com.anastasia.Anastasia_BackEnd.TestControllers;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.support.TestActivationTokenStore;
import com.anastasia.Anastasia_BackEnd.core.auth.token.Token;
import com.anastasia.Anastasia_BackEnd.core.auth.token.TokenType;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@Profile({"test", "api-tests"})  // Only available in test and API test profiles
@RestController
@RequestMapping("/api/v1/auth/test")
public class TestAuthController {

    private final TokenRepository tokenRepository;
    private final TestActivationTokenStore activationTokenStore;

    public TestAuthController(TokenRepository tokenRepository, TestActivationTokenStore activationTokenStore) {
        this.tokenRepository = tokenRepository;
        this.activationTokenStore = activationTokenStore;
    }

    // Endpoint to retrieve activation token by email
    @GetMapping("/activation-token")
    public String getActivationToken(@RequestParam String email) {
        return activationTokenStore.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No active activation token found for " + email));
    }

    @GetMapping("/refresh-token")
    public String getRefreshToken(@RequestParam String email) {
        return tokenRepository.findTopByUserEmailIgnoreCaseAndTokenTypeAndDeletedAtIsNullOrderByIdDesc(email, TokenType.REFRESH)
                .map(Token::getToken)
                .orElseThrow(() -> new RuntimeException("No refresh token found for " + email));
    }
}
