package com.anastasia.Anastasia_BackEnd.modules.dashboard.controller;

import com.anastasia.Anastasia_BackEnd.modules.dashboard.dto.TenantAdminDashboardResponse;
import com.anastasia.Anastasia_BackEnd.modules.dashboard.service.TenantAdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final TenantAdminDashboardService tenantAdminDashboardService;

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @GetMapping("/tenant-admin/summary")
    public ResponseEntity<TenantAdminDashboardResponse> getTenantAdminSummary() {
        return ResponseEntity.ok(tenantAdminDashboardService.getSummary());
    }
}
