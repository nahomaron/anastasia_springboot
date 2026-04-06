package com.anastasia.Anastasia_BackEnd.core.auth.controller;

import com.anastasia.Anastasia_BackEnd.core.auth.dto.PlatformAdminRegistrationRequest;
import com.anastasia.Anastasia_BackEnd.core.auth.service.PlatformAdminRegistrationService;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/platform-admin")
@RequiredArgsConstructor
public class PlatformAdminRegistrationController {

    public static final String DEVELOPER_SECRET_HEADER = "X-Platform-Admin-Secret";

    private final PlatformAdminRegistrationService platformAdminRegistrationService;

    /**
     * Registers a platform-level admin user that carries every role and permission. The caller
     * must supply the developer secret via the {@code X-Platform-Admin-Secret} header.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody PlatformAdminRegistrationRequest request,
                                                        @RequestHeader(DEVELOPER_SECRET_HEADER) String devSecret) {
        UserEntity created = platformAdminRegistrationService.registerPlatformAdmin(request, devSecret);
        Map<String, String> response = new LinkedHashMap<>();
        response.put("message", "Platform admin created successfully");
        response.put("userId", created.getUuid().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
