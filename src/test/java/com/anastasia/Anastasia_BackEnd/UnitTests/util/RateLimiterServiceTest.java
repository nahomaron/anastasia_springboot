package com.anastasia.Anastasia_BackEnd.UnitTests.util;

import com.anastasia.Anastasia_BackEnd.common.config.RateLimiterConfig;
import com.anastasia.Anastasia_BackEnd.common.config.RateLimiterProperties;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class RateLimiterServiceTest {

    @Mock
    private RateLimiterConfig rateLimiterConfig;
    @Mock
    private RateLimiterProperties rateLimiterProperties;
    @Mock
    private Bucket bucket;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(rateLimiterProperties.isEnabled()).thenReturn(true);
        when(rateLimiterConfig.getBucket(anyString(), anyLong(), any(Duration.class))).thenReturn(bucket);
    }

    @Test
    void isAllowed_shouldReturnTrueWhenBucketAllows() {
        when(bucket.tryConsume(1)).thenReturn(true);

        assertThat(rateLimiterService.isAllowed("key")).isTrue();
    }

    @Test
    void isAllowed_shouldReturnFalseWhenRateLimited() {
        when(bucket.tryConsume(1)).thenReturn(false);

        assertThat(rateLimiterService.isAllowed("key")).isFalse();
    }

    @Test
    void tryConsume_shouldSkipBucketsWhenDisabled() {
        when(rateLimiterProperties.isEnabled()).thenReturn(false);

        boolean allowed = rateLimiterService.tryConsume("key", 1L, Duration.ofSeconds(1));

        assertThat(allowed).isTrue();
        verify(rateLimiterConfig, never()).getBucket(anyString(), anyLong(), any(Duration.class));
    }
}
