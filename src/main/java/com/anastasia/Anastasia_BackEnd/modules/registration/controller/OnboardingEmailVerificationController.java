package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.OnboardingEmailVerificationService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/onboarding/email-verification")
public class OnboardingEmailVerificationController {

    private final OnboardingEmailVerificationService verificationService;

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, String>> sendCode(@RequestBody SendCodeRequest request) {
        verificationService.sendCode(request.email());
        return ResponseEntity.ok(Map.of("message", "Verification code sent."));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody VerifyCodeRequest request) {
        boolean verified = verificationService.verifyCode(request.email(), request.code());
        if (!verified) {
            return ResponseEntity.badRequest().body(Map.of(
                    "verified", false,
                    "message", "Verification code is invalid."
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
}
