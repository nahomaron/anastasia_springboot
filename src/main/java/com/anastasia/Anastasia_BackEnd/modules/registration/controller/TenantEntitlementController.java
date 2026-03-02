package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementAdministrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions")
public class TenantEntitlementController {

    private final EntitlementAdministrationService entitlementAdministrationService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/entitlements")
    public ResponseEntity<EntitlementSnapshotResponse> getMyEntitlements() {
        return ResponseEntity.ok(entitlementAdministrationService.resolveCurrentTenant());
    }
}
