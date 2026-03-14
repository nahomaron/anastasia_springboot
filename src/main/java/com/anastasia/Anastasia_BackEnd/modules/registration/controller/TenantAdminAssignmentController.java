package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.ChangeTenantAdminRoleRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.InviteTenantAdminRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.SetTenantBillingContactRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.TenantAdminAssignmentResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantAdminAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenant/admin-assignments")
public class TenantAdminAssignmentController {

    private final TenantAdminAssignmentService tenantAdminAssignmentService;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS', 'VIEW_TENANT_USERS')")
    @GetMapping
    public ResponseEntity<List<TenantAdminAssignmentResponse>> listAssignments() {
        UUID tenantId = requireTenantId();
        List<TenantAdminAssignmentResponse> response = tenantAdminAssignmentService.listMembers(tenantId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS', 'MANAGE_TENANT_USERS')")
    @PostMapping
    public ResponseEntity<TenantAdminAssignmentResponse> inviteAssignment(
            @Valid @RequestBody InviteTenantAdminRequest request
    ) {
        UUID tenantId = requireTenantId();
        UUID actorUserId = currentActorUserId();

        TenantAdminAssignmentEntity saved = tenantAdminAssignmentService.inviteUserToTenant(
                tenantId,
                request.getUserId(),
                request.getRole(),
                actorUserId
        );
        if (Boolean.TRUE.equals(request.getBillingContact())) {
            saved = tenantAdminAssignmentService.setBillingContact(tenantId, request.getUserId(), true, actorUserId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS', 'MANAGE_TENANT_USERS')")
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<TenantAdminAssignmentResponse> activateAssignment(@PathVariable UUID userId) {
        UUID tenantId = requireTenantId();
        TenantAdminAssignmentEntity saved = tenantAdminAssignmentService.activateMembership(tenantId, userId, currentActorUserId());
        return ResponseEntity.ok(toResponse(saved));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS', 'MANAGE_TENANT_USERS')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<TenantAdminAssignmentResponse> changeRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeTenantAdminRoleRequest request
    ) {
        UUID tenantId = requireTenantId();
        TenantAdminAssignmentEntity saved = tenantAdminAssignmentService.changeRole(
                tenantId,
                userId,
                request.getRole(),
                currentActorUserId()
        );
        return ResponseEntity.ok(toResponse(saved));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_USERS', 'MANAGE_TENANT_BILLING')")
    @PatchMapping("/{userId}/billing-contact")
    public ResponseEntity<TenantAdminAssignmentResponse> setBillingContact(
            @PathVariable UUID userId,
            @Valid @RequestBody SetTenantBillingContactRequest request
    ) {
        UUID tenantId = requireTenantId();
        TenantAdminAssignmentEntity saved = tenantAdminAssignmentService.setBillingContact(
                tenantId,
                userId,
                request.getBillingContact(),
                currentActorUserId()
        );
        return ResponseEntity.ok(toResponse(saved));
    }

    private TenantAdminAssignmentResponse toResponse(TenantAdminAssignmentEntity entity) {
        return TenantAdminAssignmentResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .userId(entity.getUserId())
                .role(entity.getRole())
                .status(entity.getStatus())
                .billingContact(entity.isBillingContact())
                .createdByUserId(entity.getCreatedByUserId())
                .updatedByUserId(entity.getUpdatedByUserId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing");
        }
        return tenantId;
    }

    private UUID currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserUuid();
        }
        return null;
    }
}
