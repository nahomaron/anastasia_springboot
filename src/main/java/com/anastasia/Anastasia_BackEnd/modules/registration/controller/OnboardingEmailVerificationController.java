package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingEmailVerificationService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/onboarding/email-verification")
public class OnboardingEmailVerificationController {

    private static final Duration SEND_CODE_PERIOD = Duration.ofMinutes(15);
    private static final Duration VERIFY_CODE_PERIOD = Duration.ofMinutes(10);

    private final OnboardingEmailVerificationService verificationService;
    private final RateLimiterService rateLimiterService;

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, String>> sendCode(
            @RequestBody SendCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!consumeRateLimit("onboarding:email-verification:send", httpRequest, request.email(), 3, SEND_CODE_PERIOD)) {
            return tooManyRequests();
        }
        verificationService.sendCode(request.email());
        return ResponseEntity.ok(Map.of("message", "Verification code sent."));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestBody VerifyCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!consumeRateLimit("onboarding:email-verification:verify", httpRequest, request.email(), 5, VERIFY_CODE_PERIOD)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "verified", false,
                    "message", "Too many requests, try again later"
            ));
        }
        boolean verified = verificationService.verifyCode(request.email(), request.code());
        if (!verified) {
            return ResponseEntity.ok(Map.of(
                    "verified", false,
                    "message", "Unable to verify email with the provided code."
            ));
        }
        return ResponseEntity.ok(Map.of(
                "verified", true,
                "message", "Email verified successfully."
        ));
    }

    public record SendCodeRequest(
            @NotBlank @Email String email
    ) {}

    public record VerifyCodeRequest(
            @NotBlank @Email String email,
            @NotBlank String code
    ) {}

    private ResponseEntity<Map<String, String>> tooManyRequests() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", "Too many requests, try again later"));
    }

    private boolean consumeRateLimit(
            String scope,
            HttpServletRequest request,
            String subject,
            long capacity,
            Duration period
    ) {
        String clientIp = request != null ? normalizeKeyComponent(request.getRemoteAddr()) : "n/a";
        String normalizedSubject = normalizeKeyComponent(subject);
        String bucketKey = scope + ":" + clientIp + ":" + normalizedSubject;
        return rateLimiterService.tryConsume(bucketKey, capacity, period);
    }

    private String normalizeKeyComponent(String value) {
        if (value == null || value.isBlank()) {
            return "anonymous";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
