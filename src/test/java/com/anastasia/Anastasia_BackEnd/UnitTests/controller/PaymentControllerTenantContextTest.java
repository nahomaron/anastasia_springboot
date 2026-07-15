package com.anastasia.Anastasia_BackEnd.UnitTests.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.query.PaymentQueryService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.query.SubscriptionQueryService;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.saga.PaymentCheckoutSaga;
import com.anastasia.Anastasia_BackEnd.modules.payments.application.usecase.CreateSubscriptionUseCase;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.PaymentController;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.PaymentQueryController;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.controller.SubscriptionQueryController;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateIntentRequest;
import com.anastasia.Anastasia_BackEnd.modules.payments.web.dto.CreateSubscriptionRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.EntitlementResolverService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentControllerTenantContextTest {

    private final LocalizedMessageService messageService = mock(LocalizedMessageService.class);
    private final PaymentCheckoutSaga checkoutSaga = mock(PaymentCheckoutSaga.class);
    private final CreateSubscriptionUseCase createSubscriptionUseCase = mock(CreateSubscriptionUseCase.class);
    private final EntitlementResolverService entitlementResolverService = mock(EntitlementResolverService.class);
    private final PaymentQueryService paymentQueryService = mock(PaymentQueryService.class);
    private final SubscriptionQueryService subscriptionQueryService = mock(SubscriptionQueryService.class);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPaymentIntent_shouldRejectWhenTenantContextMissing() {
        PaymentController controller = paymentController();

        assertForbiddenTenantContext(() -> controller.create("idem-1", new CreateIntentRequest()));
        verifyNoInteractions(checkoutSaga);
    }

    @Test
    void createSubscription_shouldRejectWhenTenantContextMissing() {
        PaymentController controller = paymentController();

        assertForbiddenTenantContext(() -> controller.createSubscription("idem-1", new CreateSubscriptionRequest()));
        verifyNoInteractions(createSubscriptionUseCase);
    }

    @Test
    void paymentQuery_shouldRejectWhenTenantContextMissing() {
        PaymentQueryController controller = new PaymentQueryController(paymentQueryService, messageService);

        assertForbiddenTenantContext(() -> controller.findAll(Pageable.unpaged()));
        assertForbiddenTenantContext(() -> controller.findById(UUID.randomUUID()));
        assertForbiddenTenantContext(controller::totalPerFund);
        verifyNoInteractions(paymentQueryService);
    }

    @Test
    void subscriptionQuery_shouldRejectWhenTenantContextMissing() {
        SubscriptionQueryController controller = new SubscriptionQueryController(subscriptionQueryService, messageService);

        assertForbiddenTenantContext(() -> controller.findAll(Pageable.unpaged()));
        assertForbiddenTenantContext(() -> controller.findById(UUID.randomUUID()));
        verifyNoInteractions(subscriptionQueryService);
    }

    @Test
    void createPaymentIntent_shouldRejectWhenStewardshipFeatureDisabled() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        when(entitlementResolverService.hasFeature(tenantId, TenantFeature.STEWARDSHIP_GIVING)).thenReturn(false);
        PaymentController controller = paymentController();

        assertThatThrownBy(() -> controller.create("idem-1", new CreateIntentRequest()))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verifyNoInteractions(checkoutSaga);
    }

    private PaymentController paymentController() {
        return new PaymentController(
                checkoutSaga,
                createSubscriptionUseCase,
                entitlementResolverService,
                messageService
        );
    }

    private void assertForbiddenTenantContext(ThrowingRunnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
