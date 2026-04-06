package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.TestDataUtil;
import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.saga.PaymentCheckoutSaga;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreateSubscriptionUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateIntentRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateSubscriptionRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementResolverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Integration Tests")
@Feature("Payments API")
@SpringBootTest
@AutoConfigureMockMvc
@Tag("experimental")
class PaymentControllerIT extends PostgresTestContainer {

    private static final UUID USER_ID = UUID.fromString("c2da4a08-4f1a-4b8b-8c74-3f5d5bd7412a");
    private static final String IDEMPOTENCY_INTENT = "intent-idem";
    private static final String IDEMPOTENCY_SUBSCRIPTION = "sub-idem";

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PaymentCheckoutSaga checkoutSaga;
    @MockitoBean private CreateSubscriptionUseCase createSubscriptionUseCase;
    @MockitoBean private EntitlementResolverService entitlementResolverService;
    @MockitoBean private LocalizedMessageService messageService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        TenantEntity tenant = tenantRepository.save(TestDataUtil.createTestTenantEntity());
        tenantId = tenant.getId();
        TenantContext.setTenantId(tenantId);
        when(entitlementResolverService.hasFeature(eq(tenantId), eq(TenantFeature.STEWARDSHIP_GIVING))).thenReturn(true);
        when(messageService.get(any(), any())).thenReturn("stub");
        when(messageService.get(any(), any(), any())).thenReturn("stub");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(authorities = "MANAGE_FINANCE")
    void createIntent_returnsCreatedResponse() throws Exception {
        var intent = PaymentIntent.newInitiated(
                tenantId,
                PaymentPurpose.TITHE,
                5_000L,
                "USD",
                11L,
                USER_ID,
                "donor@example.org",
                "12",
                IDEMPOTENCY_INTENT);
        intent.attachCheckoutSession("chk_123", "https://checkout.example.org/flow");

        when(checkoutSaga.startCheckout(
                eq(tenantId),
                eq(PaymentPurpose.TITHE),
                eq(5_000L),
                eq("USD"),
                eq(11L),
                eq(USER_ID),
                eq("donor@example.org"),
                eq("12"),
                eq(IDEMPOTENCY_INTENT)
        )).thenReturn(intent);

        CreateIntentRequest request = new CreateIntentRequest();
        request.setPurpose(PaymentPurpose.TITHE);
        request.setAmount(5_000L);
        request.setCurrency("USD");
        request.setMemberId(11L);
        request.setUserId(USER_ID);
        request.setUserEmail("donor@example.org");
        request.setFundId("12");

        mockMvc.perform(post("/api/v1/payments/intents")
                        .header("Idempotency-Key", IDEMPOTENCY_INTENT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(intent.getId().toString()))
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.example.org/flow"));
    }

    @Test
    @WithMockUser(authorities = "OWN_SUBSCRIPTION")
    void createSubscription_returnsCreatedResponse() throws Exception {
        var subscription = PaymentSubscription.newPending(
                tenantId,
                PaymentPurpose.DONATION,
                12_000L,
                "USD",
                "120",
                USER_ID,
                "giver@example.org",
                "12",
                IDEMPOTENCY_SUBSCRIPTION);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.attachCheckoutSession("sub_chk_999", "https://checkout.example.org/subscription");

        when(createSubscriptionUseCase.execute(
                eq(tenantId),
                eq(PaymentPurpose.DONATION),
                eq(12_000L),
                eq("USD"),
                eq("120"),
                eq(USER_ID),
                eq("giver@example.org"),
                eq("12"),
                eq(IDEMPOTENCY_SUBSCRIPTION)
        )).thenReturn(subscription);

        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setPurpose(PaymentPurpose.DONATION);
        request.setAmount(12_000L);
        request.setCurrency("USD");
        request.setMemberId("120");
        request.setUserId(USER_ID);
        request.setUserEmail("giver@example.org");
        request.setFundId("12");

        mockMvc.perform(post("/api/v1/payments/subscriptions")
                        .header("Idempotency-Key", IDEMPOTENCY_SUBSCRIPTION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").value(subscription.getId().toString()))
                .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.example.org/subscription"));
    }
}
