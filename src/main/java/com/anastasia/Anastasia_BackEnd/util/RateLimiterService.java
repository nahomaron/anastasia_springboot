package com.anastasia.Anastasia_BackEnd.util;

import com.anastasia.Anastasia_BackEnd.config.RateLimiterConfig;
import io.github.bucket4j.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimiterConfig rateLimiterConfig;

    public boolean isAllowed(String key) {
        return rateLimiterConfig.getBucket(key).tryConsume(1);
    }
}
