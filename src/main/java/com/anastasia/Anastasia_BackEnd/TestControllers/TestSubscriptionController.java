package com.anastasia.Anastasia_BackEnd.TestControllers;

import com.anastasia.Anastasia_BackEnd.TestSupport.TestSmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Profile({"test", "api-tests"})
@RequestMapping("/api/v1/tenant/test")
public class TestSubscriptionController {

    private final TestSmsService testSmsService;

    @GetMapping("/otp")
    public ResponseEntity<String> getLatestOtp(@RequestParam String phone) {
        if (!StringUtils.hasText(phone)) {
            return ResponseEntity.badRequest().body("Phone number is required");
        }

        return testSmsService.getLastOtpForPhone(phone)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No OTP found for phone " + phone));
    }

    @DeleteMapping("/otp")
    public ResponseEntity<Void> clearOtp(@RequestParam String phone) {
        if (!StringUtils.hasText(phone)) {
            return ResponseEntity.badRequest().build();
        }
        testSmsService.clearOtp(phone);
        return ResponseEntity.noContent().build();
    }
}
