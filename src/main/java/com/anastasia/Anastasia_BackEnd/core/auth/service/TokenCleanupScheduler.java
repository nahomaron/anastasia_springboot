package com.anastasia.Anastasia_BackEnd.core.auth.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Profile("!test") // Disable this scheduler in the 'test' profile
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final TokenRepository tokenRepository;

    @Scheduled(fixedRate = 86400000) // this makes it run every 24 hours
    @Transactional
    public void cleanupTokens(){
        // mark expired tokens as expired
        tokenRepository.markExpiredTokens(Instant.now());

        // delete expired and revoked tokens
        tokenRepository.deleteExpiredAndRevokedTokens();
        log.debug("Expired and revoked tokens cleaned up");
    }
}
