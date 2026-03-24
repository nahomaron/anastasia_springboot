package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.RequestPlanChangeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SubscriptionPlanHistoryItemResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingOverviewResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.tenantadmin.UpdateCurrentTenantFeatureRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlanHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantWorkspaceLifecycleService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions")
public class TenantEntitlementController {

    private final EntitlementAdministrationService entitlementAdministrationService;
    private final SubscriptionService subscriptionService;
    private final TenantWorkspaceLifecycleService tenantWorkspaceLifecycleService;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING', 'MANAGE_FINANCE')")
    @GetMapping("/me/entitlements")
    public ResponseEntity<EntitlementSnapshotResponse> getMyEntitlements() {
        UUID tenantId = requireTenantId();
        subscriptionService.syncSubscriptionState(tenantId, currentActorUserId());
        tenantWorkspaceLifecycleService.syncTenantLifecycle(tenantId, currentActorUserId());
        subscriptionService.applyDuePendingPlanChange(tenantId, currentActorUserId());
        return ResponseEntity.ok(entitlementAdministrationService.resolveCurrentTenant());
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING', 'MANAGE_FINANCE')")
    @GetMapping("/me/billing")
    public ResponseEntity<TenantBillingOverviewResponse> getMyBillingOverview() {
        UUID tenantId = requireTenantId();
        UUID actorUserId = currentActorUserId();

        subscriptionService.syncSubscriptionState(tenantId, actorUserId);
        TenantEntity tenant = tenantWorkspaceLifecycleService.syncTenantLifecycle(tenantId, actorUserId);
        TenantSubscriptionEntity subscription = subscriptionService.applyDuePendingPlanChange(tenantId, actorUserId);
        List<SubscriptionPlanHistoryItemResponse> history = subscriptionService.listRecentPlanHistory(tenantId).stream()
                .map(this::toHistoryItem)
                .toList();

        return ResponseEntity.ok(toBillingOverview(tenant, subscription, history));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING', 'MANAGE_FINANCE')")
    @PostMapping("/me/plan-change")
    public ResponseEntity<TenantBillingOverviewResponse> requestPlanChange(
            @Valid @RequestBody RequestPlanChangeRequest request
    ) {
        if (request.getTargetPlan() != SubscriptionPlan.BASIC) {
            throw new IllegalArgumentException("Selected plan is not available for self-service changes yet");
        }
        UUID tenantId = requireTenantId();
        UUID actorUserId = currentActorUserId();

        subscriptionService.syncSubscriptionState(tenantId, actorUserId);
        subscriptionService.requestPlanChange(
                tenantId,
                request.getTargetPlan(),
                request.getTiming(),
                request.getReason(),
                actorUserId
        );
        TenantEntity tenant = tenantWorkspaceLifecycleService.syncTenantLifecycle(tenantId, actorUserId);
        TenantSubscriptionEntity subscription = subscriptionService.getByTenantId(tenantId);
        List<SubscriptionPlanHistoryItemResponse> history = subscriptionService.listRecentPlanHistory(tenantId).stream()
                .map(this::toHistoryItem)
                .toList();

        return ResponseEntity.ok(toBillingOverview(tenant, subscription, history));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_TENANT_BILLING', 'MANAGE_FINANCE')")
    @PostMapping("/me/demo/reset")
    public ResponseEntity<TenantBillingOverviewResponse> resetMyDemoWorkspace() {
        UUID tenantId = requireTenantId();
        UUID actorUserId = currentActorUserId();
        TenantEntity tenant = tenantWorkspaceLifecycleService.resetDemoWorkspace(tenantId, actorUserId);
        TenantSubscriptionEntity subscription = subscriptionService.getByTenantId(tenantId);
        List<SubscriptionPlanHistoryItemResponse> history = subscriptionService.listRecentPlanHistory(tenantId).stream()
                .map(this::toHistoryItem)
                .toList();
        return ResponseEntity.ok(toBillingOverview(tenant, subscription, history));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_TENANTS', 'OWN_SUBSCRIPTION', 'VIEW_ALL_DATA')")
    @PutMapping("/me/features/{feature}")
    public ResponseEntity<EntitlementSnapshotResponse> updateCurrentTenantFeature(
            @PathVariable TenantFeature feature,
            @Valid @RequestBody UpdateCurrentTenantFeatureRequest request
    ) {
        UUID tenantId = requireTenantId();
        return ResponseEntity.ok(
                entitlementAdministrationService.setSelfServiceFeatureEnabled(
                        tenantId,
                        feature,
                        Boolean.TRUE.equals(request.getEnabled())
                )
        );
    }

    private SubscriptionPlanHistoryItemResponse toHistoryItem(SubscriptionPlanHistoryEntity entity) {
        return SubscriptionPlanHistoryItemResponse.builder()
                .id(entity.getId())
                .oldPlan(entity.getOldPlan())
                .newPlan(entity.getNewPlan())
                .effectiveAt(entity.getEffectiveAt())
                .reason(entity.getReason())
                .actorUserId(entity.getActorUserId())
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

    private TenantBillingOverviewResponse toBillingOverview(TenantEntity tenant,
                                                            TenantSubscriptionEntity subscription,
                                                            List<SubscriptionPlanHistoryItemResponse> history) {
        return TenantBillingOverviewResponse.builder()
                .tenantId(tenant.getId())
                .currentPlan(subscription.getPlan())
                .status(subscription.getStatus())
                .billingInterval(subscription.getBillingInterval())
                .workspaceInitializationMode(tenant.getWorkspaceInitializationMode())
                .demoWorkspace(tenant.isDemoWorkspace())
                .trialStartAt(subscription.getTrialStartAt())
                .trialEndAt(subscription.getTrialEndAt())
                .currentPeriodStartAt(subscription.getCurrentPeriodStartAt())
                .currentPeriodEndAt(subscription.getCurrentPeriodEndAt())
                .gracePeriodEndsAt(subscription.getGracePeriodEndsAt())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .pendingPlan(subscription.getPendingPlan())
                .pendingPlanEffectiveAt(subscription.getPendingPlanEffectiveAt())
                .scheduledPurgeAt(tenant.getScheduledPurgeAt())
                .archiveScheduledAt(tenant.getArchiveScheduledAt())
                .archivedAt(tenant.getArchivedAt())
                .retentionWarningActive(tenantWorkspaceLifecycleService.isRetentionWarningActive(tenant, subscription, java.time.Instant.now()))
                .recentPlanHistory(history)
                .build();
    }
}
