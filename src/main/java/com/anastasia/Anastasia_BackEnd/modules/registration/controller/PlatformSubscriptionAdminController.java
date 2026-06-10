package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.CreatePromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.GrantPlanOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.PromoCodeResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.PromoRedemptionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.RedeemPromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SetFeatureOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingChargeSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingOverviewResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantBillingOverrideResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantFeatureOverrideResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantPlanGrantResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlanHistoryEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantWorkspaceLifecycleService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantDemoTemplateCloneService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementAdministrationService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.TenantBillingOverrideService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platform/subscriptions")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformSubscriptionAdminController {

    private final EntitlementAdministrationService entitlementAdministrationService;
    private final TenantDemoTemplateCloneService tenantDemoTemplateCloneService;
    private final TenantWorkspaceLifecycleService tenantWorkspaceLifecycleService;
    private final SubscriptionService subscriptionService;
    private final TenantBillingOverrideService tenantBillingOverrideService;

    @GetMapping("/demo-template")
    public ResponseEntity<java.util.Map<String, Object>> getConfiguredDemoTemplate() {
        return ResponseEntity.ok(tenantDemoTemplateCloneService.findConfiguredTemplate()
                .map(this::toDemoTemplateResponse)
                .orElseGet(() -> java.util.Map.of(
                        "configured", false
                )));
    }

    @PatchMapping("/demo-template/{tenantId}")
    public ResponseEntity<java.util.Map<String, Object>> configureDemoTemplate(@PathVariable UUID tenantId) {
        TenantEntity tenant = tenantDemoTemplateCloneService.configureTemplateTenant(tenantId);
        return ResponseEntity.ok(toDemoTemplateResponse(tenant));
    }

    @DeleteMapping("/demo-template/{tenantId}")
    public ResponseEntity<Void> clearDemoTemplate(@PathVariable UUID tenantId) {
        tenantDemoTemplateCloneService.clearConfiguredTemplate(tenantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/demo-workspace/reset")
    public ResponseEntity<java.util.Map<String, Object>> resetDemoWorkspace(@PathVariable UUID tenantId) {
        TenantEntity tenant = tenantWorkspaceLifecycleService.resetDemoWorkspace(tenantId, currentActorUserId());
        return ResponseEntity.ok(java.util.Map.of(
                "tenantId", tenant.getId(),
                "demoWorkspace", tenant.isDemoWorkspace(),
                "scheduledPurgeAt", tenant.getScheduledPurgeAt(),
                "status", tenant.getStatus()
        ));
    }

    @GetMapping("/{tenantId}/entitlements")
    public ResponseEntity<EntitlementSnapshotResponse> getTenantEntitlements(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.resolveTenant(tenantId));
    }

    @GetMapping("/{tenantId}/billing")
    public ResponseEntity<TenantBillingOverviewResponse> getTenantBillingOverview(@PathVariable UUID tenantId) {
        UUID actorUserId = currentActorUserId();
        subscriptionService.syncSubscriptionState(tenantId, actorUserId);
        TenantEntity tenant = tenantWorkspaceLifecycleService.syncTenantLifecycle(tenantId, actorUserId);
        TenantSubscriptionEntity subscription = subscriptionService.applyDuePendingPlanChange(tenantId, actorUserId);
        List<com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SubscriptionPlanHistoryItemResponse> history =
                subscriptionService.listRecentPlanHistory(tenantId).stream()
                        .map(this::toHistoryItem)
                        .toList();
        TenantBillingChargeSummaryResponse chargeSummary = tenantBillingOverrideService.calculateCharge(
                tenantId,
                subscription.getPlan(),
                Instant.now()
        );
        return ResponseEntity.ok(TenantBillingOverviewResponse.builder()
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
                .scheduledDeletionAt(tenant.getScheduledDeletionAt())
                .archiveScheduledAt(tenant.getArchiveScheduledAt())
                .archivedAt(tenant.getArchivedAt())
                .retentionWarningActive(tenantWorkspaceLifecycleService.isRetentionWarningActive(tenant, subscription, Instant.now()))
                .normalAmountMinor(chargeSummary.getNormalAmountMinor())
                .discountAmountMinor(chargeSummary.getDiscountAmountMinor())
                .effectiveAmountMinor(chargeSummary.getEffectiveAmountMinor())
                .currency(chargeSummary.getCurrency())
                .appliedBillingOverrideType(chargeSummary.getAppliedBillingOverrideType())
                .billingOverrideEndsAt(chargeSummary.getOverrideEndsAt())
                .recentPlanHistory(history)
                .build());
    }

    @GetMapping("/{tenantId}/billing-overrides/active")
    public ResponseEntity<TenantBillingOverrideResponse> getActiveBillingOverride(@PathVariable UUID tenantId) {
        return tenantBillingOverrideService.findActiveOverride(tenantId, Instant.now())
                .map(this::toBillingOverrideResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{tenantId}/billing-overrides")
    public ResponseEntity<List<TenantBillingOverrideResponse>> listBillingOverrides(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantBillingOverrideService.listOverrideHistory(tenantId).stream()
                .map(this::toBillingOverrideResponse)
                .toList());
    }

    @PostMapping("/{tenantId}/billing-overrides")
    public ResponseEntity<TenantBillingOverrideResponse> createBillingOverride(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantBillingOverrideRequest request
    ) {
        return ResponseEntity.ok(toBillingOverrideResponse(
                tenantBillingOverrideService.createOverride(tenantId, request, currentActorUserId())
        ));
    }

    @PutMapping("/{tenantId}/billing-overrides/{overrideId}")
    public ResponseEntity<TenantBillingOverrideResponse> updateBillingOverride(
            @PathVariable UUID tenantId,
            @PathVariable UUID overrideId,
            @Valid @RequestBody TenantBillingOverrideRequest request
    ) {
        return ResponseEntity.ok(toBillingOverrideResponse(
                tenantBillingOverrideService.updateOverride(tenantId, overrideId, request, currentActorUserId())
        ));
    }

    @DeleteMapping("/{tenantId}/billing-overrides/{overrideId}")
    public ResponseEntity<Void> revokeBillingOverride(
            @PathVariable UUID tenantId,
            @PathVariable UUID overrideId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        tenantBillingOverrideService.revokeOverride(tenantId, overrideId, reason, currentActorUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tenantId}/plan-overrides")
    public ResponseEntity<java.util.List<TenantPlanGrantResponse>> listPlanOverrides(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.listPlanGrants(tenantId).stream()
                .map(this::toPlanGrantResponse)
                .toList());
    }

    @GetMapping("/{tenantId}/feature-overrides")
    public ResponseEntity<java.util.List<TenantFeatureOverrideResponse>> listFeatureOverrides(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.listFeatureOverrides(tenantId).stream()
                .map(this::toFeatureOverrideResponse)
                .toList());
    }

    @PatchMapping("/{tenantId}/plan")
    public ResponseEntity<EntitlementSnapshotResponse> changeBasePlan(
            @PathVariable UUID tenantId,
            @RequestParam("plan") SubscriptionPlan plan
    ) {
        return ResponseEntity.ok(entitlementAdministrationService.changeBasePlan(tenantId, plan));
    }

    @PostMapping("/{tenantId}/plan-overrides")
    public ResponseEntity<TenantPlanGrantResponse> grantPlanOverride(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GrantPlanOverrideRequest request
    ) {
        return ResponseEntity.ok(toPlanGrantResponse(entitlementAdministrationService.grantPlanOverride(tenantId, request)));
    }

    @DeleteMapping("/{tenantId}/plan-overrides/{grantId}")
    public ResponseEntity<Void> revokePlanOverride(
            @PathVariable UUID tenantId,
            @PathVariable UUID grantId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        entitlementAdministrationService.revokePlanGrant(tenantId, grantId, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{tenantId}/feature-overrides")
    public ResponseEntity<TenantFeatureOverrideResponse> setFeatureOverride(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SetFeatureOverrideRequest request
    ) {
        return ResponseEntity.ok(toFeatureOverrideResponse(entitlementAdministrationService.setFeatureOverride(tenantId, request)));
    }

    @DeleteMapping("/{tenantId}/feature-overrides/{overrideId}")
    public ResponseEntity<Void> removeFeatureOverride(
            @PathVariable UUID tenantId,
            @PathVariable UUID overrideId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        entitlementAdministrationService.removeFeatureOverride(tenantId, overrideId, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/promo-codes")
    public ResponseEntity<PromoCodeResponse> createPromoCode(@Valid @RequestBody CreatePromoCodeRequest request) {
        return ResponseEntity.ok(toPromoCodeResponse(entitlementAdministrationService.createPromoCode(request)));
    }

    @GetMapping("/promo-codes")
    public ResponseEntity<java.util.List<PromoCodeResponse>> listPromoCodes() {
        return ResponseEntity.ok(entitlementAdministrationService.listPromoCodes().stream()
                .map(this::toPromoCodeResponse)
                .toList());
    }

    @GetMapping("/{tenantId}/promo-redemptions")
    public ResponseEntity<java.util.List<PromoRedemptionResponse>> listPromoRedemptions(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.listPromoRedemptions(tenantId).stream()
                .map(this::toPromoRedemptionResponse)
                .toList());
    }

    @PostMapping("/{tenantId}/redeem-promo")
    public ResponseEntity<EntitlementSnapshotResponse> redeemPromoCode(
            @PathVariable UUID tenantId,
            @Valid @RequestBody RedeemPromoCodeRequest request
    ) {
        return ResponseEntity.ok(entitlementAdministrationService.redeemPromoCode(tenantId, request));
    }

    @DeleteMapping("/{tenantId}/promo-redemptions/{redemptionId}")
    public ResponseEntity<EntitlementSnapshotResponse> revokePromoRedemption(
            @PathVariable UUID tenantId,
            @PathVariable UUID redemptionId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return ResponseEntity.ok(entitlementAdministrationService.revokePromoRedemption(tenantId, redemptionId, reason));
    }

    private TenantPlanGrantResponse toPlanGrantResponse(com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantPlanGrantEntity entity) {
        return TenantPlanGrantResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .grantedPlan(entity.getGrantedPlan())
                .source(entity.getSource())
                .promoCode(entity.getPromoCode())
                .activeMemberLimitOverride(entity.getActiveMemberLimitOverride())
                .active(entity.isActive())
                .startsAt(entity.getStartsAt())
                .expiresAt(entity.getExpiresAt())
                .revokedAt(entity.getRevokedAt())
                .reason(entity.getReason())
                .createdByUserId(entity.getCreatedByUserId())
                .updatedByUserId(entity.getUpdatedByUserId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TenantFeatureOverrideResponse toFeatureOverrideResponse(com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeatureOverrideEntity entity) {
        return TenantFeatureOverrideResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .feature(entity.getFeature())
                .enabled(entity.isEnabled())
                .source(entity.getSource())
                .promoCode(entity.getPromoCode())
                .active(entity.isActive())
                .startsAt(entity.getStartsAt())
                .expiresAt(entity.getExpiresAt())
                .revokedAt(entity.getRevokedAt())
                .reason(entity.getReason())
                .createdByUserId(entity.getCreatedByUserId())
                .updatedByUserId(entity.getUpdatedByUserId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PromoCodeResponse toPromoCodeResponse(com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoCodeEntity entity) {
        return PromoCodeResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .grantedPlan(entity.getGrantedPlan())
                .grantedFeatures(entity.getGrantedFeatures())
                .activeMemberLimitOverride(entity.getActiveMemberLimitOverride())
                .active(entity.isActive())
                .maxRedemptions(entity.getMaxRedemptions())
                .currentRedemptions(entity.getCurrentRedemptions())
                .oneTimePerTenant(entity.isOneTimePerTenant())
                .expiresAt(entity.getExpiresAt())
                .activatedAt(entity.getActivatedAt())
                .deactivatedAt(entity.getDeactivatedAt())
                .createdByUserId(entity.getCreatedByUserId())
                .updatedByUserId(entity.getUpdatedByUserId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private PromoRedemptionResponse toPromoRedemptionResponse(com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoRedemptionEntity entity) {
        return PromoRedemptionResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .promoCodeId(entity.getPromoCode() != null ? entity.getPromoCode().getId() : null)
                .promoCode(entity.getPromoCode() != null ? entity.getPromoCode().getCode() : null)
                .active(entity.isActive())
                .redeemedAt(entity.getRedeemedAt())
                .expiresAt(entity.getExpiresAt())
                .revokedAt(entity.getRevokedAt())
                .reason(entity.getReason())
                .createdByUserId(entity.getCreatedByUserId())
                .updatedByUserId(entity.getUpdatedByUserId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TenantBillingOverrideResponse toBillingOverrideResponse(
            com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideEntity entity
    ) {
        return TenantBillingOverrideResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .overrideType(entity.getOverrideType())
                .active(entity.isActive())
                .effective(entity.isEffective(Instant.now()))
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .discountPercent(entity.getDiscountPercent())
                .fixedAmountMinor(entity.getFixedAmountMinor())
                .currency(entity.getCurrency())
                .reason(entity.getReason())
                .internalNote(entity.getInternalNote())
                .createdByUserId(entity.getCreatedByUserId())
                .updatedByUserId(entity.getUpdatedByUserId())
                .revokedByUserId(entity.getRevokedByUserId())
                .revokedAt(entity.getRevokedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SubscriptionPlanHistoryItemResponse toHistoryItem(
            SubscriptionPlanHistoryEntity entity
    ) {
        return com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SubscriptionPlanHistoryItemResponse.builder()
                .id(entity.getId())
                .oldPlan(entity.getOldPlan())
                .newPlan(entity.getNewPlan())
                .effectiveAt(entity.getEffectiveAt())
                .reason(entity.getReason())
                .actorUserId(entity.getActorUserId())
                .build();
    }

    private java.util.Map<String, Object> toDemoTemplateResponse(TenantEntity tenant) {
        return java.util.Map.of(
                "configured", true,
                "tenantId", tenant.getId(),
                "displayName", tenant.getDisplayName(),
                "slug", tenant.getSlug(),
                "ownerEmail", tenant.getOwnerEmail(),
                "status", tenant.getStatus(),
                "demoTemplate", tenant.isDemoTemplate()
        );
    }

    private UUID currentActorUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal principal) {
            return principal.getUserUuid();
        }
        return null;
    }
}
