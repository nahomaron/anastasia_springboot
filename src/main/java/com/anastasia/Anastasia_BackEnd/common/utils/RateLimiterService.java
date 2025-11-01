package com.anastasia.Anastasia_BackEnd.common.utils;

import com.anastasia.Anastasia_BackEnd.common.config.RateLimiterConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimiterConfig rateLimiterConfig;

    public boolean isAllowed(String key) {
        return rateLimiterConfig.getBucket(key).tryConsume(1);
    }
}
