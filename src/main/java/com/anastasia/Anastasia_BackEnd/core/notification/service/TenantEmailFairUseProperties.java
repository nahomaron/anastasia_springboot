package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.template.EmailCategory;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.PlanEntitlementCatalog;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "notification.email.fair-use")
@Getter
@Setter
public class TenantEmailFairUseProperties {

    private boolean enabled = true;
    private int defaultMonthlyQuota = 1_000;
    private Set<EmailCategory> exemptCategories = EnumSet.of(
            EmailCategory.AUTH,
            EmailCategory.SECURITY,
            EmailCategory.BILLING,
            EmailCategory.SYSTEM,
            EmailCategory.COMPLIANCE,
            EmailCategory.TENANT
    );
    private Map<SubscriptionPlan, Integer> planMonthlyQuotas = defaultPlanMonthlyQuotas();

    public Integer resolvePlanQuota(SubscriptionPlan plan, PlanEntitlementCatalog catalog) {
        SubscriptionPlan normalized = catalog.normalizePlan(plan);
        Integer planQuota = planMonthlyQuotas.get(normalized);
        return planQuota != null ? planQuota : defaultMonthlyQuota;
    }

    private static Map<SubscriptionPlan, Integer> defaultPlanMonthlyQuotas() {
        Map<SubscriptionPlan, Integer> quotas = new EnumMap<>(SubscriptionPlan.class);
        quotas.put(SubscriptionPlan.FREE, 250);
        quotas.put(SubscriptionPlan.BASIC, 500);
        quotas.put(SubscriptionPlan.ADVANCED, 5_000);
        quotas.put(SubscriptionPlan.PREMIUM, 25_000);
        quotas.put(SubscriptionPlan.ENTERPRISE, 25_000);
        quotas.put(SubscriptionPlan.MONTHLY, 500);
        quotas.put(SubscriptionPlan.ANNUAL, 500);
        return quotas;
    }
}
