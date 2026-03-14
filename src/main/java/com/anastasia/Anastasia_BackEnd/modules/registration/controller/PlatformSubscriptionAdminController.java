package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.CreatePromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.GrantPlanOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.PromoCodeResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.PromoRedemptionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.RedeemPromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SetFeatureOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantFeatureOverrideResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.TenantPlanGrantResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/platform/subscriptions")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformSubscriptionAdminController {

    private final EntitlementAdministrationService entitlementAdministrationService;

    @GetMapping("/{tenantId}/entitlements")
    public ResponseEntity<EntitlementSnapshotResponse> getTenantEntitlements(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.resolveTenant(tenantId));
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
}
