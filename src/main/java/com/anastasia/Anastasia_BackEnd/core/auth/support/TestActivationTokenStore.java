package com.anastasia.Anastasia_BackEnd.core.auth.support;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"test", "api-tests"})
public class TestActivationTokenStore implements ActivationTokenObserver {

    private final Map<String, String> latestTokensByEmail = new ConcurrentHashMap<>();

    @Override
    public void record(String email, String rawToken) {
        if (email == null || rawToken == null) {
            return;
        }
        latestTokensByEmail.put(email.trim().toLowerCase(), rawToken);
    }

    public Optional<String> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(latestTokensByEmail.get(email.trim().toLowerCase()));
    }
}
