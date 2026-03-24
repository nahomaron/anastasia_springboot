package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.CurrentTenantSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateCurrentTenantSettingsRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant/settings")
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANTS', 'OWN_SUBSCRIPTION', 'VIEW_ALL_DATA')")
    @GetMapping
    public ResponseEntity<CurrentTenantSettingsResponse> getCurrentTenantSettings() {
        return ResponseEntity.ok(tenantSettingsService.getCurrentTenantSettings());
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANTS', 'OWN_SUBSCRIPTION', 'VIEW_ALL_DATA')")
    @PutMapping
    public ResponseEntity<CurrentTenantSettingsResponse> updateCurrentTenantSettings(
            @Valid @RequestBody UpdateCurrentTenantSettingsRequest request
    ) {
        return ResponseEntity.ok(tenantSettingsService.updateCurrentTenantSettings(request));
    }
}
