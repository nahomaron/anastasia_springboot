package com.anastasia.Anastasia_BackEnd.common.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
public class RateLimiterConfig {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Bean
    public ConcurrentMap<String, Bucket> buckets() {
        return buckets;
    }

    public Bucket getBucket(String key) {
        return getBucket(key, 5, Duration.ofMinutes(1));
    }

    public Bucket getBucket(String key, long capacity, Duration refillPeriod) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, refillPeriod)))
                .build());
    }

    @Bean
    public Bucket defaultBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build();
    }
}
