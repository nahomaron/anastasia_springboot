package com.anastasia.Anastasia_BackEnd.UnitTests.config;

import com.anastasia.Anastasia_BackEnd.config.RateLimiterConfig;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterConfigTest {

    private RateLimiterConfig config;

    @BeforeEach
    void setUp() {
        config = new RateLimiterConfig();
    }

    @Test
    void getBucket_shouldCreateAndReuseBucketPerKey() {
        Bucket firstCall = config.getBucket("127.0.0.1");
        Bucket secondCall = config.getBucket("127.0.0.1");

        assertThat(firstCall).isSameAs(secondCall);

        for (int i = 0; i < 5; i++) {
            assertThat(firstCall.tryConsume(1)).isTrue();
        }
        assertThat(firstCall.tryConsume(1)).isFalse();
    }

    @Test
    void bucketsBean_shouldExposeUnderlyingMap() {
        ConcurrentMap<String, Bucket> bean = config.buckets();
        Bucket bucket = config.getBucket("client");

        assertThat(bean).containsKey("client");
        assertThat(bean.get("client")).isSameAs(bucket);
    }

    @Test
    void defaultBucketBean_shouldProvideConfiguredBucket() {
        Bucket defaultBucket = config.defaultBucket();

        assertThat(defaultBucket).isNotNull();
        assertThat(defaultBucket.tryConsume(5)).isTrue();
        assertThat(defaultBucket.tryConsume(1)).isFalse();
    }
}
