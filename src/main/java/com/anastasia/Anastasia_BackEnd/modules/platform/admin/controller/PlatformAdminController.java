package com.anastasia.Anastasia_BackEnd.modules.platform.admin.controller;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSettingsResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSettingsUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformPaymentRecordResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformPaymentStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformPriestApplicationResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformPriestAssignmentRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformPriestStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformSupportTicketResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformTenantRowResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformTenantStatusUpdateRequest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.dto.PlatformAdminSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminActionService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminReportService;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminSettingsService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final PlatformAdminReportService reportService;
    private final PlatformAdminSettingsService settingsService;
    private final PlatformAdminActionService actionService;

    @GetMapping("/summary")
    public ResponseEntity<PlatformAdminSummaryResponse> summary() {
        return ResponseEntity.ok(reportService.getSummary());
    }

    @GetMapping("/tenants")
    public ResponseEntity<List<PlatformTenantRowResponse>> tenants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) SubscriptionPlan plan,
            @RequestParam(required = false, defaultValue = "12") Integer limit
    ) {
        return ResponseEntity.ok(reportService.listTenants(search, status, plan, limit));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PlatformPaymentRecordResponse>> payments(
            @RequestParam(required = false) String tenant,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "12") Integer limit
    ) {
        return ResponseEntity.ok(reportService.listPayments(tenant, status, limit));
    }

    @GetMapping("/priests")
    public ResponseEntity<List<PlatformPriestApplicationResponse>> priests() {
        return ResponseEntity.ok(reportService.listPriests());
    }

    @GetMapping("/support/tickets")
    public ResponseEntity<List<PlatformSupportTicketResponse>> tickets() {
        return ResponseEntity.ok(reportService.listSupportTickets());
    }

    @PutMapping("/tenants/{tenantId}/status")
    public ResponseEntity<Void> updateTenantStatus(
            @PathVariable UUID tenantId,
            @Valid @RequestBody PlatformTenantStatusUpdateRequest request
    ) {
        actionService.updateTenantStatus(tenantId, TenantStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT)), currentActorUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/payments/{paymentId}/retry")
    public ResponseEntity<Void> retryPayment(@PathVariable UUID paymentId) {
        actionService.retryPayment(paymentId, currentActorUserId());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/payments/{paymentId}/status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody PlatformPaymentStatusUpdateRequest request
    ) {
        if ("REFUNDED".equalsIgnoreCase(request.getStatus())) {
            actionService.refundPayment(paymentId, currentActorUserId());
            return ResponseEntity.accepted().build();
        }
        throw new IllegalArgumentException("Unsupported status: " + request.getStatus());
    }

    @PostMapping("/priests/{priestId}/assign")
    public ResponseEntity<Void> assignPriest(
            @PathVariable Long priestId,
            @Valid @RequestBody PlatformPriestAssignmentRequest request
    ) {
        actionService.assignPriest(priestId, request.getTenantId(), currentActorUserId());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/priests/{priestId}/status")
    public ResponseEntity<Void> updatePriestStatus(
            @PathVariable Long priestId,
            @Valid @RequestBody PlatformPriestStatusUpdateRequest request
    ) {
        actionService.updatePriestStatus(priestId, request.getStatus());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<PlatformAdminSettingsResponse> settings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<PlatformAdminSettingsResponse> updateSettings(@Valid @RequestBody PlatformAdminSettingsUpdateRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(request));
    }

    private UUID currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserUuid();
        }
        return null;
    }
}
