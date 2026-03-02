package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.CreatePromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.GrantPlanOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.RedeemPromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SetFeatureOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoRedemptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeatureOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantPlanGrantEntity;
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
    public ResponseEntity<java.util.List<TenantPlanGrantEntity>> listPlanOverrides(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.listPlanGrants(tenantId));
    }

    @GetMapping("/{tenantId}/feature-overrides")
    public ResponseEntity<java.util.List<TenantFeatureOverrideEntity>> listFeatureOverrides(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.listFeatureOverrides(tenantId));
    }

    @PatchMapping("/{tenantId}/plan")
    public ResponseEntity<EntitlementSnapshotResponse> changeBasePlan(
            @PathVariable UUID tenantId,
            @RequestParam("plan") SubscriptionPlan plan
    ) {
        return ResponseEntity.ok(entitlementAdministrationService.changeBasePlan(tenantId, plan));
    }

    @PostMapping("/{tenantId}/plan-overrides")
    public ResponseEntity<TenantPlanGrantEntity> grantPlanOverride(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GrantPlanOverrideRequest request
    ) {
        return ResponseEntity.ok(entitlementAdministrationService.grantPlanOverride(tenantId, request));
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
    public ResponseEntity<TenantFeatureOverrideEntity> setFeatureOverride(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SetFeatureOverrideRequest request
    ) {
        return ResponseEntity.ok(entitlementAdministrationService.setFeatureOverride(tenantId, request));
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
    public ResponseEntity<PromoCodeEntity> createPromoCode(@Valid @RequestBody CreatePromoCodeRequest request) {
        return ResponseEntity.ok(entitlementAdministrationService.createPromoCode(request));
    }

    @GetMapping("/promo-codes")
    public ResponseEntity<java.util.List<PromoCodeEntity>> listPromoCodes() {
        return ResponseEntity.ok(entitlementAdministrationService.listPromoCodes());
    }

    @GetMapping("/{tenantId}/promo-redemptions")
    public ResponseEntity<java.util.List<PromoRedemptionEntity>> listPromoRedemptions(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(entitlementAdministrationService.listPromoRedemptions(tenantId));
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
}
