package com.anastasia.Anastasia_BackEnd.core.auth.controller;

import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditOutcome;
import com.anastasia.Anastasia_BackEnd.core.auth.audit.PlatformAdminBootstrapAuditService;
import com.anastasia.Anastasia_BackEnd.core.auth.dto.PlatformAdminRegistrationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.service.PlatformAdminRegistrationService;
import com.anastasia.Anastasia_BackEnd.core.auth.service.exception.InvalidPlatformAdminBootstrapSecretException;
import com.anastasia.Anastasia_BackEnd.core.auth.service.exception.PlatformAdminBootstrapCompletedException;
import com.anastasia.Anastasia_BackEnd.core.auth.service.exception.PlatformAdminBootstrapDisabledException;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/platform-admin")
@RequiredArgsConstructor
public class PlatformAdminRegistrationController {

    public static final String DEVELOPER_SECRET_HEADER = "X-Platform-Admin-Secret";
    private static final long RATE_LIMIT_CAPACITY = 3L;
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofMinutes(15);

    private final PlatformAdminRegistrationService platformAdminRegistrationService;
    private final PlatformAdminBootstrapAuditService auditService;
    private final RateLimiterService rateLimiterService;

    /**
     * Registers the first platform-level admin user during controlled bootstrap only.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody PlatformAdminRegistrationRequest request,
                                                        @RequestHeader(DEVELOPER_SECRET_HEADER) String devSecret,
                                                        HttpServletRequest httpRequest) {
        String attemptedEmail = normalizeEmail(request.getEmail());
        String clientIp = resolveClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String rateLimitKey = "auth:platform-admin-bootstrap:" + clientIp + ":" + normalizeKeyComponent(attemptedEmail);

        if (!rateLimiterService.tryConsume(rateLimitKey, RATE_LIMIT_CAPACITY, RATE_LIMIT_PERIOD)) {
            auditService.recordAttempt(
                    attemptedEmail,
                    PlatformAdminBootstrapAuditOutcome.RATE_LIMITED,
                    "Platform admin bootstrap rate limit exceeded",
                    clientIp,
                    userAgent,
                    null
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many requests, try again later"));
        }

        try {
            UserEntity created = platformAdminRegistrationService.registerPlatformAdmin(request, devSecret);
            auditService.recordAttempt(
                    attemptedEmail,
                    PlatformAdminBootstrapAuditOutcome.SUCCESS,
                    "Platform admin bootstrap completed successfully",
                    clientIp,
                    userAgent,
                    created.getUuid()
            );
            Map<String, String> response = new LinkedHashMap<>();
            response.put("message", "Platform admin bootstrap completed successfully");
            response.put("userId", created.getUuid().toString());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException ex) {
            auditService.recordAttempt(
                    attemptedEmail,
                    mapOutcome(ex),
                    ex.getMessage(),
                    clientIp,
                    userAgent,
                    null
            );
            throw ex;
        }
    }

    private PlatformAdminBootstrapAuditOutcome mapOutcome(RuntimeException ex) {
        if (ex instanceof InvalidPlatformAdminBootstrapSecretException) {
            return PlatformAdminBootstrapAuditOutcome.INVALID_SECRET;
        }
        if (ex instanceof PlatformAdminBootstrapDisabledException) {
            return PlatformAdminBootstrapAuditOutcome.BOOTSTRAP_DISABLED;
        }
        if (ex instanceof PlatformAdminBootstrapCompletedException) {
            return PlatformAdminBootstrapAuditOutcome.BOOTSTRAP_ALREADY_COMPLETED;
        }
        if (ex instanceof IllegalArgumentException && ex.getMessage() != null
                && ex.getMessage().toLowerCase(Locale.ROOT).contains("already exists")) {
            return PlatformAdminBootstrapAuditOutcome.DUPLICATE_EMAIL;
        }
        return PlatformAdminBootstrapAuditOutcome.FAILED;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeKeyComponent(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return "unknown";
        }
        return normalizeKeyComponent(remoteAddress);
    }
}
