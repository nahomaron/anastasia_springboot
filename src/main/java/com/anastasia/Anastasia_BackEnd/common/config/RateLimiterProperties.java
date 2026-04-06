package com.anastasia.Anastasia_BackEnd.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties backing the rate limiter feature flag.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /**
     * Flag that enables or disables rate limiting globally.
     */
    private boolean enabled = true;
}
