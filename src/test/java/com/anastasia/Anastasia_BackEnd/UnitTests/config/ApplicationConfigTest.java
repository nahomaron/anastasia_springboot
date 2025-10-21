package com.anastasia.Anastasia_BackEnd.UnitTests.config;

import com.anastasia.Anastasia_BackEnd.config.ApplicationConfig;
import com.anastasia.Anastasia_BackEnd.auditing.ApplicationAuditAware;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigTest {

    private final ApplicationConfig config = new ApplicationConfig();

    @Test
    void taskExecutor_shouldUseConfiguredThreadPoolSettings() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.taskExecutor();

        assertThat(executor.getCorePoolSize()).isEqualTo(8);
        assertThat(executor.getMaxPoolSize()).isEqualTo(10);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("AsyncExecutor-");
    }
}
