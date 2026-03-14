package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.RequestPlanChangeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SubscriptionPlanHistoryItemResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingOverviewResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlanHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') "
            + "or @permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_FINANCE')")
    @GetMapping("/me/entitlements")
    public ResponseEntity<EntitlementSnapshotResponse> getMyEntitlements() {
        UUID tenantId = requireTenantId();
        subscriptionService.applyDuePendingPlanChange(tenantId, currentActorUserId());
        return ResponseEntity.ok(entitlementAdministrationService.resolveCurrentTenant());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') "
            + "or @permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_FINANCE')")
    @GetMapping("/me/billing")
    public ResponseEntity<TenantBillingOverviewResponse> getMyBillingOverview() {
        UUID tenantId = requireTenantId();
        UUID actorUserId = currentActorUserId();

        TenantSubscriptionEntity subscription = subscriptionService.applyDuePendingPlanChange(tenantId, actorUserId);
        List<SubscriptionPlanHistoryItemResponse> history = subscriptionService.listRecentPlanHistory(tenantId).stream()
                .map(this::toHistoryItem)
                .toList();

        return ResponseEntity.ok(TenantBillingOverviewResponse.builder()
                .tenantId(tenantId)
                .currentPlan(subscription.getPlan())
                .status(subscription.getStatus())
                .billingInterval(subscription.getBillingInterval())
                .currentPeriodStartAt(subscription.getCurrentPeriodStartAt())
                .currentPeriodEndAt(subscription.getCurrentPeriodEndAt())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .pendingPlan(subscription.getPendingPlan())
                .pendingPlanEffectiveAt(subscription.getPendingPlanEffectiveAt())
                .recentPlanHistory(history)
                .build());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') "
            + "or @permissionEvaluator.hasAny(authentication, 'OWN_SUBSCRIPTION', 'MANAGE_FINANCE')")
    @PostMapping("/me/plan-change")
    public ResponseEntity<TenantBillingOverviewResponse> requestPlanChange(
            @Valid @RequestBody RequestPlanChangeRequest request
    ) {
        UUID tenantId = requireTenantId();
        UUID actorUserId = currentActorUserId();

        subscriptionService.requestPlanChange(
                tenantId,
                request.getTargetPlan(),
                request.getTiming(),
                request.getReason(),
                actorUserId
        );
        TenantSubscriptionEntity subscription = subscriptionService.getByTenantId(tenantId);
        List<SubscriptionPlanHistoryItemResponse> history = subscriptionService.listRecentPlanHistory(tenantId).stream()
                .map(this::toHistoryItem)
                .toList();

        return ResponseEntity.ok(TenantBillingOverviewResponse.builder()
                .tenantId(tenantId)
                .currentPlan(subscription.getPlan())
                .status(subscription.getStatus())
                .billingInterval(subscription.getBillingInterval())
                .currentPeriodStartAt(subscription.getCurrentPeriodStartAt())
                .currentPeriodEndAt(subscription.getCurrentPeriodEndAt())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .pendingPlan(subscription.getPendingPlan())
                .pendingPlanEffectiveAt(subscription.getPendingPlanEffectiveAt())
                .recentPlanHistory(history)
                .build());
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
}
