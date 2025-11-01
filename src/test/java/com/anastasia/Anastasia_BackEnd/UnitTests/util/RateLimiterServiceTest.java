package com.anastasia.Anastasia_BackEnd.UnitTests.util;

import com.anastasia.Anastasia_BackEnd.common.config.RateLimiterConfig;
import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RateLimiterConfig rateLimiterConfig;
    @Mock
    private Bucket bucket;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(rateLimiterConfig.getBucket("key")).thenReturn(bucket);
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
}
