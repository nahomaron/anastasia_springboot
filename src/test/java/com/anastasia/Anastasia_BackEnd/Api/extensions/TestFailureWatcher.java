package com.anastasia.Anastasia_BackEnd.Api.extensions;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.utils.TestContextHolder;
import com.anastasia.Anastasia_BackEnd.Api.utils.TestDataManager;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Global JUnit 5 TestWatcher that listens for test outcomes.
 * If a test fails, it triggers conditional cleanup through TestDataManager.
 */
public class TestFailureWatcher implements TestWatcher {

    private static final Logger log = LoggerFactory.getLogger(TestFailureWatcher.class);

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testName = context.getDisplayName();
        String email = Optional.ofNullable(TestContextHolder.getEmail())
                .orElse(BaseApiTest.getCachedEmail());

        log.error("Test '{}' failed with exception: {}", testName, cause.getMessage());
        TestDataManager.cleanupOnFailure(email, true);
        TestContextHolder.clear();
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        log.info("Test '{}' passed successfully.", context.getDisplayName());
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        log.warn("Test '{}' aborted: {}", context.getDisplayName(), cause.getMessage());
    }
}
