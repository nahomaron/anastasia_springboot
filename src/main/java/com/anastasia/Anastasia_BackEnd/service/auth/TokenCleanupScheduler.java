package com.anastasia.Anastasia_BackEnd.service.auth;

import com.anastasia.Anastasia_BackEnd.repository.auth.TokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!test") // Disable this scheduler in the 'test' profile
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenRepository tokenRepository;

    @Scheduled(fixedRate = 86400000) // this makes it run every 24 hours
    @Transactional
    public void cleanupTokens(){
        // mark expired tokens as expired
        tokenRepository.markExpiredTokens();

        // delete expired and revoked tokens
        tokenRepository.deleteExpiredAndRevokedTokens();
        System.out.println("Expired and revoked tokens cleaned up");
    }
}
