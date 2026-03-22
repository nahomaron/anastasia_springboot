package com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class PlanEntitlementCatalog {

    public static final String LIMIT_ACTIVE_MEMBERS = "ACTIVE_MEMBERS";
    public static final int LAUNCH_BASIC_ACTIVE_MEMBER_LIMIT = 100;

    private final Map<SubscriptionPlan, PlanDefinition> definitions = new EnumMap<>(SubscriptionPlan.class);

    public PlanEntitlementCatalog() {
        PlanDefinition launchBasic = new PlanDefinition(
                Set.of(
                        TenantFeature.MEMBER_MANAGEMENT,
                        TenantFeature.CALENDAR,
                        TenantFeature.APPOINTMENTS,
                        TenantFeature.SACRAMENTAL_SERVICES,
                        TenantFeature.EVENTS,
                        TenantFeature.EVENT_ATTENDANCE,
                        TenantFeature.GROUPS,
                        TenantFeature.NOTIFICATIONS,
                        TenantFeature.REPORTING
                ),
                LAUNCH_BASIC_ACTIVE_MEMBER_LIMIT
        );

        // FREE is a time-limited Basic trial for launch.
        definitions.put(SubscriptionPlan.FREE, launchBasic);
        definitions.put(SubscriptionPlan.BASIC, launchBasic);

        definitions.put(SubscriptionPlan.ADVANCED, new PlanDefinition(
                Set.of(
                        TenantFeature.MEMBER_MANAGEMENT,
                        TenantFeature.CALENDAR,
                        TenantFeature.APPOINTMENTS,
                        TenantFeature.SACRAMENTAL_SERVICES,
                        TenantFeature.EVENTS,
                        TenantFeature.EVENT_ATTENDANCE,
                        TenantFeature.GROUPS,
                        TenantFeature.NOTIFICATIONS,
                        TenantFeature.EMAILING,
                        TenantFeature.STEWARDSHIP_GIVING,
                        TenantFeature.REPORTING,
                        TenantFeature.FINANCE_ACCOUNTING,
                        TenantFeature.ANALYTICS
                ),
                5000
        ));

        definitions.put(SubscriptionPlan.PREMIUM, new PlanDefinition(
                Set.of(TenantFeature.values()),
                25000
        ));

        // Backward-compatibility aliases for legacy values in existing data.
        definitions.put(SubscriptionPlan.ENTERPRISE, definitions.get(SubscriptionPlan.PREMIUM));
        definitions.put(SubscriptionPlan.MONTHLY, definitions.get(SubscriptionPlan.BASIC));
        definitions.put(SubscriptionPlan.ANNUAL, definitions.get(SubscriptionPlan.BASIC));
    }

    public PlanDefinition definitionFor(SubscriptionPlan plan) {
        SubscriptionPlan normalized = plan == null ? SubscriptionPlan.FREE : plan;
        return definitions.getOrDefault(normalized, definitions.get(SubscriptionPlan.FREE));
    }

    public SubscriptionPlan normalizePlan(SubscriptionPlan plan) {
        if (plan == null) {
            return SubscriptionPlan.FREE;
        }
        if (plan == SubscriptionPlan.ENTERPRISE) {
            return SubscriptionPlan.PREMIUM;
        }
        if (plan == SubscriptionPlan.MONTHLY || plan == SubscriptionPlan.ANNUAL) {
            return SubscriptionPlan.BASIC;
        }
        return plan;
    }

    public record PlanDefinition(
            Set<TenantFeature> features,
            int activeMembersLimit
    ) {
    }
}
