package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.Console;

@Component
@RequiredArgsConstructor
public class PlatformAdminRecoveryCommand {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminRecoveryCommand.class);

    @Value("${app.platform-admin.recovery.issue-token.enabled:false}")
    private boolean recoveryEnabled;
    @Value("${app.platform-admin.recovery.issue-token.email:}")
    private String recoveryEmail;
    @Value("${app.platform-admin.recovery.issue-token.reason:}")
    private String recoveryReason;
    @Value("${app.platform-admin.recovery.issue-token.operator:system}")
    private String recoveryOperator;
    @Value("${app.platform-admin.recovery.issue-token.exit-after-run:true}")
    private boolean exitAfterRun;

    private final PlatformAdminRecoveryService recoveryService;
    private final ConfigurableApplicationContext applicationContext;

    @EventListener(ApplicationReadyEvent.class)
    public void runRecoveryIfRequested() {
        if (!recoveryEnabled) {
            return;
        }

        int exitCode = 0;
        try {
            PlatformAdminRecoveryTokenResult result = recoveryService.issueOperatorRecoveryToken(
                    recoveryEmail,
                    recoveryOperator,
                    recoveryReason
            );
            log.warn("Platform admin recovery token issued for {}", result.email());
            writeResetUrlToConsole(result);
            log.warn("Expires at: {}", result.expiresAt());
        } catch (RuntimeException ex) {
            exitCode = 1;
            log.error("Platform admin recovery token issuance failed: {}", ex.getMessage(), ex);
        }

        if (exitAfterRun) {
            int finalExitCode = exitCode;
            Thread shutdownThread = new Thread(() -> {
                int computedExitCode = SpringApplication.exit(applicationContext, () -> finalExitCode);
                log.info("Platform admin recovery command requested shutdown with exit code {}", computedExitCode);
            }, "platform-admin-recovery-exit");
            shutdownThread.setDaemon(false);
            shutdownThread.start();
        }
    }

    void writeResetUrlToConsole(PlatformAdminRecoveryTokenResult result) {
        Console console = locateConsole();
        if (console == null) {
            log.warn("Platform admin recovery reset URL generated for {} but withheld because no interactive console is attached", result.email());
            return;
        }

        console.printf("%nPlatform admin reset URL for %s%n%s%nExpires at: %s%n",
                result.email(),
                result.resetUrl(),
                result.expiresAt());
    }

    Console locateConsole() {
        return System.console();
    }
}
