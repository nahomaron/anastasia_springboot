package com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement.EntitlementSnapshotResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoRedemptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeatureOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantPlanGrantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PromoRedemptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantFeatureOverrideRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantPlanGrantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntitlementResolverService {

    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantPlanGrantRepository tenantPlanGrantRepository;
    private final TenantFeatureOverrideRepository tenantFeatureOverrideRepository;
    private final PromoRedemptionRepository promoRedemptionRepository;
    private final PlanEntitlementCatalog catalog;

    public EntitlementSnapshotResponse resolve(UUID tenantId) {
        LocalDateTime now = LocalDateTime.now();
        SubscriptionPlan basePlan = tenantSubscriptionRepository.findByTenantId(tenantId)
                .map(subscription -> subscription.getPlan())
                .orElse(SubscriptionPlan.FREE);
        SubscriptionPlan effectivePlan = catalog.normalizePlan(basePlan);

        List<TenantPlanGrantEntity> planGrants = tenantPlanGrantRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
        Integer activeMemberLimitOverride = null;

        for (TenantPlanGrantEntity grant : planGrants) {
            if (!grant.isEffective(now) || grant.getGrantedPlan() == null) {
                continue;
            }
            SubscriptionPlan grantPlan = catalog.normalizePlan(grant.getGrantedPlan());
            if (grantPlan.rank() > effectivePlan.rank()) {
                effectivePlan = grantPlan;
            }
            if (grant.getActiveMemberLimitOverride() != null) {
                activeMemberLimitOverride = max(activeMemberLimitOverride, grant.getActiveMemberLimitOverride());
            }
        }

        Set<TenantFeature> features = EnumSet.noneOf(TenantFeature.class);
        features.addAll(catalog.definitionFor(effectivePlan).features());

        List<PromoRedemptionEntity> redemptions = promoRedemptionRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
        for (PromoRedemptionEntity redemption : redemptions) {
            if (!isPromoEffective(redemption, now)) {
                continue;
            }
            if (redemption.getPromoCode().getGrantedFeatures() != null) {
                features.addAll(redemption.getPromoCode().getGrantedFeatures());
            }
            if (redemption.getPromoCode().getActiveMemberLimitOverride() != null) {
                activeMemberLimitOverride = max(activeMemberLimitOverride, redemption.getPromoCode().getActiveMemberLimitOverride());
            }
        }

        List<TenantFeatureOverrideEntity> featureOverrides = tenantFeatureOverrideRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
        for (TenantFeatureOverrideEntity override : featureOverrides) {
            if (!override.isEffective(now) || override.getFeature() == null) {
                continue;
            }
            if (override.isEnabled()) {
                features.add(override.getFeature());
            } else {
                features.remove(override.getFeature());
            }
        }

        int activeMemberLimit = catalog.definitionFor(effectivePlan).activeMembersLimit();
        if (activeMemberLimitOverride != null) {
            activeMemberLimit = Math.max(activeMemberLimit, activeMemberLimitOverride);
        }

        Map<String, Integer> limits = new HashMap<>();
        limits.put(PlanEntitlementCatalog.LIMIT_ACTIVE_MEMBERS, activeMemberLimit);

        return EntitlementSnapshotResponse.builder()
                .tenantId(tenantId)
                .basePlan(basePlan)
                .effectivePlan(effectivePlan)
                .features(features)
                .limits(limits)
                .build();
    }

    public boolean hasFeature(UUID tenantId, TenantFeature feature) {
        return resolve(tenantId).getFeatures().contains(feature);
    }

    public int activeMembersLimit(UUID tenantId) {
        return resolve(tenantId).getLimits().getOrDefault(PlanEntitlementCatalog.LIMIT_ACTIVE_MEMBERS, 0);
    }

    private boolean isPromoEffective(PromoRedemptionEntity redemption, LocalDateTime now) {
        if (!redemption.isActive() || redemption.getRevokedAt() != null) {
            return false;
        }
        if (redemption.getExpiresAt() != null && !redemption.getExpiresAt().isAfter(now)) {
            return false;
        }
        return redemption.getPromoCode() != null
                && redemption.getPromoCode().isActive()
                && (redemption.getPromoCode().getExpiresAt() == null || redemption.getPromoCode().getExpiresAt().isAfter(now));
    }

    private Integer max(Integer left, Integer right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return Math.max(left, right);
    }
}
