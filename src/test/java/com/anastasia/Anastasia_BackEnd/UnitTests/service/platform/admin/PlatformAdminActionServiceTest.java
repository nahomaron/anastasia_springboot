package com.anastasia.Anastasia_BackEnd.UnitTests.service.platform.admin;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformAdminActionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionPlan;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.service.PlatformSupportAccessService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
@Tag("experimental")
class PlatformAdminActionServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantSubscriptionEventRepository eventRepository;
    @Mock private SubscriptionService subscriptionService;
    @Mock private PriestRepository priestRepository;
    @Mock private PlatformSupportAccessService supportAccessService;

    @InjectMocks private PlatformAdminActionService actionService;

    @Test
    void updateTenantStatus_shouldSuspendAndClearSuspensionReason() {
        UUID tenantId = UUID.randomUUID();
        TenantEntity tenant = new TenantEntity();
        tenant.setId(tenantId);
        tenant.setStatus(TenantStatus.ACTIVE);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        actionService.updateTenantStatus(tenantId, TenantStatus.SUSPENDED, UUID.randomUUID());

        ArgumentCaptor<TenantEntity> captor = ArgumentCaptor.forClass(TenantEntity.class);
        verify(tenantRepository).save(captor.capture());
        TenantEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(saved.getSuspensionReason()).isEqualTo("Suspended by platform admin");
        assertThat(saved.getSuspendedAt()).isNotNull();
    }

    @Test
    void retryPayment_onlyAllowsFailedEvents() {
        UUID paymentId = UUID.randomUUID();
        TenantEntity tenant = new TenantEntity();
        tenant.setId(UUID.randomUUID());
        TenantSubscriptionEventEntity event = new TenantSubscriptionEventEntity();
        event.setEventType(TenantSubscriptionEventType.PAYMENT_FAILED);
        event.setTenant(tenant);
        when(eventRepository.findById(paymentId)).thenReturn(Optional.of(event));

        actionService.retryPayment(paymentId, UUID.randomUUID());

        verify(subscriptionService).recordPaymentSucceeded(eq(tenant.getId()), any(), any(), any(), any(), any());
    }

    @Test
    void refundPayment_savesRefundEvent() {
        UUID paymentId = UUID.randomUUID();
        TenantEntity tenant = new TenantEntity();
        tenant.setId(UUID.randomUUID());
        TenantSubscriptionEntity subscription = new TenantSubscriptionEntity();
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        TenantSubscriptionEventEntity event = new TenantSubscriptionEventEntity();
        event.setTenant(tenant);
        event.setTenantSubscription(subscription);
        event.setEventType(TenantSubscriptionEventType.PAYMENT_SUCCEEDED);
        when(eventRepository.findById(paymentId)).thenReturn(Optional.of(event));

        actionService.refundPayment(paymentId, UUID.randomUUID());

        ArgumentCaptor<TenantSubscriptionEventEntity> captor = ArgumentCaptor.forClass(TenantSubscriptionEventEntity.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(TenantSubscriptionEventType.PAYMENT_REFUNDED);
    }

    @Test
    void assignPriest_updatesTenantAndPriest() {
        Long priestId = 5L;
        UUID tenantId = UUID.randomUUID();
        PriestEntity priest = new PriestEntity();
        priest.setId(priestId);
        TenantEntity tenant = new TenantEntity();
        tenant.setId(tenantId);
        when(priestRepository.findById(priestId)).thenReturn(Optional.of(priest));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        actionService.assignPriest(priestId, tenantId, UUID.randomUUID());

        verify(priestRepository).save(priest);
        assertThat(priest.getTenant()).isEqualTo(tenant);
        assertThat(priest.isActive()).isTrue();
        assertThat(tenant.getUpdatedBy()).isNotNull();
    }

    @Test
    void updatePriestStatus_flipsActiveFlag() {
        Long priestId = 9L;
        UUID actorUserId = UUID.randomUUID();
        PriestEntity priest = new PriestEntity();
        priest.setId(priestId);
        priest.setStatus(PriestStatus.PENDING);
        priest.setActive(true);
        when(priestRepository.findById(priestId)).thenReturn(Optional.of(priest));

        actionService.updatePriestStatus(priestId, PriestStatus.INACTIVE, actorUserId);

        verify(priestRepository, times(1)).save(priest);
        assertThat(priest.isActive()).isFalse();
        assertThat(priest.getStatus()).isEqualTo(PriestStatus.INACTIVE);
    }
}
