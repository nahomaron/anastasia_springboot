package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;
import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingInterval;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingOverrideType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantBillingOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantBillingOverrideRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "billing.tenant.currency=USD",
        "billing.tenant.plans.FREE.priceId=price_free_test",
        "billing.tenant.plans.FREE.amountMinor=0",
        "billing.tenant.plans.BASIC.priceId=price_basic_test",
        "billing.tenant.plans.BASIC.amountMinor=5000"
})
class PlatformSubscriptionAdminBillingOverrideControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private TenantSubscriptionRepository tenantSubscriptionRepository;
    @Autowired private TenantBillingOverrideRepository tenantBillingOverrideRepository;
    @Autowired private TenantService tenantService;

    private TenantEntity tenant;
    private TenantSubscriptionEntity subscription;

    @BeforeEach
    void setUp() {
        TenantDTO dto = TestDataUtil.createTestTenantDTO();
        tenant = tenantRepository.save(tenantService.convertTenantToEntity(dto));
        subscription = tenantSubscriptionRepository.save(TenantSubscriptionEntity.builder()
                .tenant(tenant)
                .plan(SubscriptionPlan.BASIC)
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval(BillingInterval.MONTHLY)
                .provider(BillingProvider.MANUAL)
                .currentPeriodStartAt(Instant.now().minusSeconds(86400))
                .currentPeriodEndAt(Instant.now().plusSeconds(86400 * 30L))
                .startedAt(Instant.now().minusSeconds(86400))
                .build());
        tenant.setSubscription(subscription);
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdminCanManageBillingOverrides() throws Exception {
        var request = TenantBillingOverrideRequestBody.percentDiscount();

        String response = mockMvc.perform(post("/api/v1/platform/subscriptions/{tenantId}/billing-overrides", tenant.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overrideType").value("PERCENT_DISCOUNT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String overrideId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/v1/platform/subscriptions/{tenantId}/billing-overrides/active", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(overrideId))
                .andExpect(jsonPath("$.effective").value(true));

        mockMvc.perform(get("/api/v1/platform/subscriptions/{tenantId}/billing", tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPlan").value("BASIC"))
                .andExpect(jsonPath("$.normalAmountMinor").value(5000))
                .andExpect(jsonPath("$.discountAmountMinor").value(1250))
                .andExpect(jsonPath("$.effectiveAmountMinor").value(3750))
                .andExpect(jsonPath("$.appliedBillingOverrideType").value("PERCENT_DISCOUNT"));

        mockMvc.perform(delete("/api/v1/platform/subscriptions/{tenantId}/billing-overrides/{overrideId}", tenant.getId(), overrideId)
                        .with(csrf())
                        .param("reason", "Offer ended"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/platform/subscriptions/{tenantId}/billing-overrides/active", tenant.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "PLATFORM_ADMIN")
    void platformAdminCannotCreateOverlappingActiveOverrides() throws Exception {
        tenantBillingOverrideRepository.save(TenantBillingOverrideEntity.builder()
                .tenant(tenant)
                .overrideType(BillingOverrideType.FREE_ACCESS)
                .active(true)
                .startsAt(Instant.parse("2026-06-01T00:00:00Z"))
                .endsAt(Instant.parse("2026-07-01T00:00:00Z"))
                .build());

        var request = TenantBillingOverrideRequestBody.percentDiscount();

        mockMvc.perform(post("/api/v1/platform/subscriptions/{tenantId}/billing-overrides", tenant.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "OWN_SUBSCRIPTION")
    void tenantScopedUserCannotManageBillingOverrides() throws Exception {
        mockMvc.perform(post("/api/v1/platform/subscriptions/{tenantId}/billing-overrides", tenant.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TenantBillingOverrideRequestBody.percentDiscount())))
                .andExpect(status().isForbidden());
    }

    private static final class TenantBillingOverrideRequestBody {
        private final BillingOverrideType overrideType;
        private final Instant startsAt;
        private final Instant endsAt;
        private final BigDecimal discountPercent;
        private final Long fixedAmountMinor;
        private final String currency;
        private final String reason;
        private final String internalNote;

        private TenantBillingOverrideRequestBody(BillingOverrideType overrideType,
                                                 Instant startsAt,
                                                 Instant endsAt,
                                                 BigDecimal discountPercent,
                                                 Long fixedAmountMinor,
                                                 String currency,
                                                 String reason,
                                                 String internalNote) {
            this.overrideType = overrideType;
            this.startsAt = startsAt;
            this.endsAt = endsAt;
            this.discountPercent = discountPercent;
            this.fixedAmountMinor = fixedAmountMinor;
            this.currency = currency;
            this.reason = reason;
            this.internalNote = internalNote;
        }

        static TenantBillingOverrideRequestBody percentDiscount() {
            return new TenantBillingOverrideRequestBody(
                    BillingOverrideType.PERCENT_DISCOUNT,
                    Instant.parse("2026-06-01T00:00:00Z"),
                    Instant.parse("2026-07-01T00:00:00Z"),
                    BigDecimal.valueOf(25),
                    null,
                    "USD",
                    "Early adopter discount",
                    "Internal approval note"
            );
        }

        public BillingOverrideType getOverrideType() {
            return overrideType;
        }

        public Instant getStartsAt() {
            return startsAt;
        }

        public Instant getEndsAt() {
            return endsAt;
        }

        public BigDecimal getDiscountPercent() {
            return discountPercent;
        }

        public Long getFixedAmountMinor() {
            return fixedAmountMinor;
        }

        public String getCurrency() {
            return currency;
        }

        public String getReason() {
            return reason;
        }

        public String getInternalNote() {
            return internalNote;
        }
    }
}
