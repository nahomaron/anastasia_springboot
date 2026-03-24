package com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.CreatePromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.GrantPlanOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.RedeemPromoCodeRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.SetFeatureOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.EntitlementActionType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.GrantSource;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoCodeEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoRedemptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntitlementAuditEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeatureOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantPlanGrantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PromoCodeRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PromoRedemptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantEntitlementAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantFeatureOverrideRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantPlanGrantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntitlementAdministrationService {

    private final TenantRepository tenantRepository;
    private final SubscriptionService subscriptionService;
    private final PromoCodeRepository promoCodeRepository;
    private final PromoRedemptionRepository promoRedemptionRepository;
    private final TenantPlanGrantRepository tenantPlanGrantRepository;
    private final TenantFeatureOverrideRepository tenantFeatureOverrideRepository;
    private final TenantEntitlementAuditRepository tenantEntitlementAuditRepository;
    private final EntitlementResolverService entitlementResolverService;
    private final PlanEntitlementCatalog planEntitlementCatalog;
    private final LocalizedMessageService messageService;

    @Transactional(readOnly = true)
    public EntitlementSnapshotResponse resolveCurrentTenant() {
        UUID tenantId = requireTenantId();
        return entitlementResolverService.resolve(tenantId);
    }

    @Transactional(readOnly = true)
    public EntitlementSnapshotResponse resolveTenant(UUID tenantId) {
        requireTenant(tenantId);
        return entitlementResolverService.resolve(tenantId);
    }

    @Transactional(readOnly = true)
    public java.util.List<TenantPlanGrantEntity> listPlanGrants(UUID tenantId) {
        requireTenant(tenantId);
        return tenantPlanGrantRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public java.util.List<TenantFeatureOverrideEntity> listFeatureOverrides(UUID tenantId) {
        requireTenant(tenantId);
        return tenantFeatureOverrideRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public java.util.List<PromoCodeEntity> listPromoCodes() {
        return promoCodeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public java.util.List<PromoRedemptionEntity> listPromoRedemptions(UUID tenantId) {
        requireTenant(tenantId);
        return promoRedemptionRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public EntitlementSnapshotResponse changeBasePlan(UUID tenantId, SubscriptionPlan plan) {
        UUID actorUserId = currentActorUserId();
        subscriptionService.changePlan(tenantId, plan, actorUserId);
        audit(tenantId, EntitlementActionType.PLAN_CHANGED, "Base plan changed to " + plan, actorUserId);
        return entitlementResolverService.resolve(tenantId);
    }

    @Transactional
    public TenantPlanGrantEntity grantPlanOverride(UUID tenantId, @Valid GrantPlanOverrideRequest request) {
        UUID actorUserId = currentActorUserId();
        TenantEntity tenant = requireTenant(tenantId);

        TenantPlanGrantEntity grant = TenantPlanGrantEntity.builder()
                .tenant(tenant)
                .grantedPlan(planEntitlementCatalog.normalizePlan(request.getPlan()))
                .source(GrantSource.MANUAL)
                .activeMemberLimitOverride(request.getActiveMemberLimitOverride())
                .startsAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .reason(request.getReason())
                .createdByUserId(actorUserId)
                .updatedByUserId(actorUserId)
                .build();
        TenantPlanGrantEntity saved = tenantPlanGrantRepository.save(grant);
        audit(tenantId, EntitlementActionType.PLAN_OVERRIDE_GRANTED,
                "Manual plan override " + saved.getGrantedPlan(), actorUserId);
        return saved;
    }

    @Transactional
    public void revokePlanGrant(UUID tenantId, UUID grantId, String reason) {
        UUID actorUserId = currentActorUserId();
        TenantPlanGrantEntity grant = tenantPlanGrantRepository.findById(grantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.entitlement.planGrant.notFound",
                        "Plan grant not found"
                )));
        if (!grant.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.entitlement.planGrant.tenantMismatch",
                    "Plan grant does not belong to tenant"
            ));
        }
        grant.setActive(false);
        grant.setRevokedAt(Instant.now());
        grant.setExpiresAt(Instant.now());
        grant.setReason(reason);
        grant.setUpdatedByUserId(actorUserId);
        tenantPlanGrantRepository.save(grant);
        audit(tenantId, EntitlementActionType.PLAN_OVERRIDE_REVOKED,
                "Plan override revoked: " + grantId, actorUserId);
    }

    @Transactional
    public TenantFeatureOverrideEntity setFeatureOverride(UUID tenantId, @Valid SetFeatureOverrideRequest request) {
        UUID actorUserId = currentActorUserId();
        TenantEntity tenant = requireTenant(tenantId);

        TenantFeatureOverrideEntity override = TenantFeatureOverrideEntity.builder()
                .tenant(tenant)
                .feature(request.getFeature())
                .enabled(Boolean.TRUE.equals(request.getEnabled()))
                .source(GrantSource.MANUAL)
                .active(true)
                .startsAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .reason(request.getReason())
                .createdByUserId(actorUserId)
                .updatedByUserId(actorUserId)
                .build();
        TenantFeatureOverrideEntity saved = tenantFeatureOverrideRepository.save(override);
        audit(tenantId, EntitlementActionType.FEATURE_OVERRIDE_SET,
                "Feature override: " + request.getFeature() + " -> " + request.getEnabled(), actorUserId);
        return saved;
    }

    @Transactional
    public EntitlementSnapshotResponse setSelfServiceFeatureEnabled(UUID tenantId, TenantFeature feature, boolean enabled) {
        UUID actorUserId = currentActorUserId();
        TenantEntity tenant = requireTenant(tenantId);
        Instant now = Instant.now();

        java.util.Set<TenantFeature> planScopedFeatures =
                entitlementResolverService.resolvePlanScopedFeatures(tenantId);
        java.util.List<TenantFeatureOverrideEntity> existingOverrides =
                tenantFeatureOverrideRepository.findByTenant_IdAndFeatureOrderByCreatedAtDesc(tenantId, feature);

        if (enabled) {
            if (!planScopedFeatures.contains(feature)) {
                throw new IllegalArgumentException(messageService.get(
                        "tenant.entitlement.feature.notIncluded",
                        "Feature is not available on the current tenant plan."
                ));
            }

            existingOverrides.stream()
                    .filter(override -> override.isEffective(now) && override.getSource() == GrantSource.MANUAL)
                    .forEach(override -> {
                        override.setActive(false);
                        override.setRevokedAt(now);
                        override.setExpiresAt(now);
                        override.setReason("Re-enabled by tenant admin");
                        override.setUpdatedByUserId(actorUserId);
                        tenantFeatureOverrideRepository.save(override);
                    });
        } else {
            existingOverrides.stream()
                    .filter(override -> override.isEffective(now) && override.getSource() == GrantSource.MANUAL)
                    .forEach(override -> {
                        override.setActive(false);
                        override.setRevokedAt(now);
                        override.setExpiresAt(now);
                        override.setReason("Replaced by tenant admin feature setting");
                        override.setUpdatedByUserId(actorUserId);
                        tenantFeatureOverrideRepository.save(override);
                    });

            tenantFeatureOverrideRepository.save(TenantFeatureOverrideEntity.builder()
                    .tenant(tenant)
                    .feature(feature)
                    .enabled(false)
                    .source(GrantSource.MANUAL)
                    .active(true)
                    .startsAt(now)
                    .reason("Disabled by tenant admin")
                    .createdByUserId(actorUserId)
                    .updatedByUserId(actorUserId)
                    .build());
        }

        audit(tenantId, EntitlementActionType.FEATURE_OVERRIDE_SET,
                "Self-service feature setting: " + feature + " -> " + enabled, actorUserId);
        return entitlementResolverService.resolve(tenantId);
    }

    @Transactional
    public void removeFeatureOverride(UUID tenantId, UUID overrideId, String reason) {
        UUID actorUserId = currentActorUserId();
        TenantFeatureOverrideEntity override = tenantFeatureOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.entitlement.featureOverride.notFound",
                        "Feature override not found"
                )));
        if (!override.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.entitlement.featureOverride.tenantMismatch",
                    "Feature override does not belong to tenant"
            ));
        }
        override.setActive(false);
        override.setRevokedAt(Instant.now());
        override.setExpiresAt(Instant.now());
        override.setReason(reason);
        override.setUpdatedByUserId(actorUserId);
        tenantFeatureOverrideRepository.save(override);
        audit(tenantId, EntitlementActionType.FEATURE_OVERRIDE_REMOVED,
                "Feature override removed: " + override.getFeature(), actorUserId);
    }

    @Transactional
    public PromoCodeEntity createPromoCode(@Valid CreatePromoCodeRequest request) {
        UUID actorUserId = currentActorUserId();
        String normalizedCode = normalizeCode(request.getCode());
        if (promoCodeRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.entitlement.promoCode.alreadyExists",
                    "Promo code already exists"
            ));
        }
        PromoCodeEntity promoCode = PromoCodeEntity.builder()
                .code(normalizedCode)
                .name(request.getName().trim())
                .description(request.getDescription())
                .grantedPlan(request.getGrantedPlan())
                .grantedFeatures(request.getGrantedFeatures() == null ? java.util.Set.of() : request.getGrantedFeatures())
                .activeMemberLimitOverride(request.getActiveMemberLimitOverride())
                .maxRedemptions(request.getMaxRedemptions())
                .oneTimePerTenant(request.isOneTimePerTenant())
                .expiresAt(request.getExpiresAt())
                .active(true)
                .activatedAt(Instant.now())
                .createdByUserId(actorUserId)
                .updatedByUserId(actorUserId)
                .build();
        return promoCodeRepository.save(promoCode);
    }

    @Transactional
    public EntitlementSnapshotResponse redeemPromoCode(UUID tenantId, @Valid RedeemPromoCodeRequest request) {
        UUID actorUserId = currentActorUserId();
        TenantEntity tenant = requireTenant(tenantId);
        PromoCodeEntity promoCode = promoCodeRepository.findByCodeIgnoreCase(normalizeCode(request.getCode()))
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.entitlement.promoCode.notFound",
                        "Promo code not found"
                )));

        Instant now = Instant.now();
        if (!promoCode.isRedeemableAt(now)) {
            throw new IllegalStateException(messageService.get(
                    "tenant.entitlement.promoCode.inactiveOrExpired",
                    "Promo code is not active or has expired"
            ));
        }
        if (promoCode.getMaxRedemptions() != null && promoCode.getCurrentRedemptions() >= promoCode.getMaxRedemptions()) {
            throw new IllegalStateException(messageService.get(
                    "tenant.entitlement.promoCode.redemptionLimitReached",
                    "Promo code redemption limit reached"
            ));
        }
        if (promoCode.isOneTimePerTenant() &&
                promoRedemptionRepository.existsByTenant_IdAndPromoCode_IdAndActiveTrue(tenantId, promoCode.getId())) {
            throw new IllegalStateException(messageService.get(
                    "tenant.entitlement.promoCode.alreadyRedeemed",
                    "Promo code already redeemed for this tenant"
            ));
        }

        PromoRedemptionEntity redemption = PromoRedemptionEntity.builder()
                .tenant(tenant)
                .promoCode(promoCode)
                .active(true)
                .redeemedAt(now)
                .expiresAt(promoCode.getExpiresAt())
                .reason(request.getReason())
                .createdByUserId(actorUserId)
                .updatedByUserId(actorUserId)
                .build();
        promoRedemptionRepository.save(redemption);

        if (promoCode.getGrantedPlan() != null) {
            tenantPlanGrantRepository.save(TenantPlanGrantEntity.builder()
                    .tenant(tenant)
                    .grantedPlan(planEntitlementCatalog.normalizePlan(promoCode.getGrantedPlan()))
                    .source(GrantSource.PROMO)
                    .promoCode(promoCode.getCode())
                    .activeMemberLimitOverride(promoCode.getActiveMemberLimitOverride())
                    .startsAt(now)
                    .expiresAt(promoCode.getExpiresAt())
                    .reason(request.getReason())
                    .createdByUserId(actorUserId)
                    .updatedByUserId(actorUserId)
                    .build());
        }

        if (promoCode.getGrantedFeatures() != null) {
            promoCode.getGrantedFeatures().forEach(feature -> tenantFeatureOverrideRepository.save(
                    TenantFeatureOverrideEntity.builder()
                            .tenant(tenant)
                            .feature(feature)
                            .enabled(true)
                            .source(GrantSource.PROMO)
                            .promoCode(promoCode.getCode())
                            .active(true)
                            .startsAt(now)
                            .expiresAt(promoCode.getExpiresAt())
                            .reason(request.getReason())
                            .createdByUserId(actorUserId)
                            .updatedByUserId(actorUserId)
                            .build()
            ));
        }

        promoCode.setCurrentRedemptions(promoCode.getCurrentRedemptions() + 1);
        promoCode.setUpdatedByUserId(actorUserId);
        promoCodeRepository.save(promoCode);

        audit(tenantId, EntitlementActionType.PROMO_REDEEMED,
                "Promo redeemed: " + promoCode.getCode(), actorUserId);
        return entitlementResolverService.resolve(tenantId);
    }

    @Transactional
    public EntitlementSnapshotResponse revokePromoRedemption(UUID tenantId, UUID redemptionId, String reason) {
        UUID actorUserId = currentActorUserId();
        PromoRedemptionEntity redemption = promoRedemptionRepository.findById(redemptionId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.entitlement.promoRedemption.notFound",
                        "Promo redemption not found"
                )));
        if (!redemption.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.entitlement.promoRedemption.tenantMismatch",
                    "Promo redemption does not belong to tenant"
            ));
        }
        redemption.setActive(false);
        redemption.setRevokedAt(Instant.now());
        redemption.setReason(reason);
        redemption.setUpdatedByUserId(actorUserId);
        promoRedemptionRepository.save(redemption);

        String promoCode = redemption.getPromoCode().getCode();
        Instant now = Instant.now();
        tenantPlanGrantRepository.findByTenant_IdAndPromoCodeIgnoreCase(tenantId, promoCode)
                .forEach(grant -> {
                    grant.setActive(false);
                    grant.setRevokedAt(now);
                    grant.setExpiresAt(now);
                    grant.setReason(reason);
                    grant.setUpdatedByUserId(actorUserId);
                    tenantPlanGrantRepository.save(grant);
                });

        tenantFeatureOverrideRepository.findByTenant_IdAndPromoCodeIgnoreCase(tenantId, promoCode)
                .forEach(override -> {
                    override.setActive(false);
                    override.setRevokedAt(now);
                    override.setExpiresAt(now);
                    override.setReason(reason);
                    override.setUpdatedByUserId(actorUserId);
                    tenantFeatureOverrideRepository.save(override);
                });

        audit(tenantId, EntitlementActionType.PROMO_REVOKED, "Promo revoked: " + promoCode, actorUserId);
        return entitlementResolverService.resolve(tenantId);
    }

    private TenantEntity requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.notFound",
                        "Tenant not found"
                )));
    }

    private UUID requireTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "tenant.context.missing",
                    "Tenant context is missing"
            ));
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

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void audit(UUID tenantId, EntitlementActionType actionType, String details, UUID actorUserId) {
        TenantEntity tenant = requireTenant(tenantId);
        tenantEntitlementAuditRepository.save(TenantEntitlementAuditEntity.builder()
                .tenant(tenant)
                .action(actionType)
                .details(details)
                .actorUserId(actorUserId)
                .build());
    }

}
