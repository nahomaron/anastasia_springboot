package com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "billing.tenant")
@Getter
@Setter
@RequiredArgsConstructor
public class TenantPlanBillingCatalog {

    private String currency = "USD";
    private Map<SubscriptionPlan, PlanPrice> plans = new EnumMap<>(SubscriptionPlan.class);
    private final LocalizedMessageService messageService;

    public PlanPrice resolve(SubscriptionPlan plan) {
        PlanPrice definition = plans.get(plan);
        if (definition == null) {
            throw new IllegalStateException(messageService.get(
                    "billing.plan.price.missing",
                    "No billing price configured for plan {0}",
                    plan
            ));
        }
        if (definition.getPriceId() == null || definition.getPriceId().isBlank()) {
            throw new IllegalStateException(messageService.get(
                    "billing.plan.priceId.missing",
                    "Stripe priceId is missing for plan {0}",
                    plan
            ));
        }
        if (definition.getAmountMinor() == null || definition.getAmountMinor() < 0) {
            throw new IllegalStateException(messageService.get(
                    "billing.plan.amount.invalid",
                    "amountMinor is invalid for plan {0}",
                    plan
            ));
        }
        return definition;
    }

    @Getter
    @Setter
    public static class PlanPrice {
        private String priceId;
        private Long amountMinor;
    }
}
