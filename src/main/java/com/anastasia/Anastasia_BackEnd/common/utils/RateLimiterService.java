package com.anastasia.Anastasia_BackEnd.common.utils;

import com.anastasia.Anastasia_BackEnd.common.config.RateLimiterConfig;
import com.anastasia.Anastasia_BackEnd.common.config.RateLimiterProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final long DEFAULT_CAPACITY = 5L;
    private static final Duration DEFAULT_REFILL_PERIOD = Duration.ofMinutes(1);

    private final RateLimiterConfig rateLimiterConfig;
    private final RateLimiterProperties rateLimiterProperties;

    public boolean isAllowed(String key) {
        return tryConsume(key, DEFAULT_CAPACITY, DEFAULT_REFILL_PERIOD);
    }

    public boolean tryConsume(String key, long capacity, Duration refillPeriod) {
        if (!rateLimiterProperties.isEnabled()) {
            return true;
        }
        return rateLimiterConfig.getBucket(key, capacity, refillPeriod).tryConsume(1);
    }
}
