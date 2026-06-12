package com.anastasia.Anastasia_BackEnd.modules.platform.admin.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventType;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessScope;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.SubscriptionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformAdminActionService {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionEventRepository eventRepository;
    private final SubscriptionService subscriptionService;
    private final PriestRepository priestRepository;
    private final PlatformSupportAccessService supportAccessService;

    @Transactional
    public void updateTenantStatus(UUID tenantId, TenantStatus targetStatus, UUID actorUserId) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        supportAccessService.authorizeCurrentRequestSession(actorUserId, tenantId, SupportAccessScope.READ_WRITE);
        if (targetStatus != TenantStatus.ACTIVE && targetStatus != TenantStatus.SUSPENDED) {
            throw new IllegalArgumentException("Unsupported tenant status: " + targetStatus);
        }
        tenant.setStatus(targetStatus);
        tenant.setUpdatedBy(actorUserId);
        Instant now = Instant.now();
        if (targetStatus == TenantStatus.SUSPENDED) {
            tenant.setSuspendedAt(now);
            tenant.setSuspensionReason("Suspended by platform admin");
        } else {
            tenant.setSuspensionReason(null);
            tenant.setSuspendedAt(null);
            tenant.setActivatedAt(now);
        }
        tenantRepository.save(tenant);
    }

    @Transactional
    public void retryPayment(UUID paymentId, UUID actorUserId) {
        TenantSubscriptionEventEntity event = eventRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment event not found"));
        supportAccessService.authorizeCurrentRequestSession(actorUserId, event.getTenant().getId(), SupportAccessScope.READ_WRITE);
        if (event.getEventType() != TenantSubscriptionEventType.PAYMENT_FAILED) {
            throw new IllegalStateException("Only failed payments can be retried");
        }
        UUID tenantId = event.getTenant().getId();
        subscriptionService.recordPaymentSucceeded(tenantId, Instant.now(), null, null, null, actorUserId);
    }

    @Transactional
    public void refundPayment(UUID paymentId, UUID actorUserId) {
        TenantSubscriptionEventEntity event = eventRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment event not found"));
        supportAccessService.authorizeCurrentRequestSession(actorUserId, event.getTenant().getId(), SupportAccessScope.READ_WRITE);
        TenantSubscriptionEventEntity refundEvent = new TenantSubscriptionEventEntity();
        refundEvent.setTenant(event.getTenant());
        refundEvent.setTenantSubscription(event.getTenantSubscription());
        refundEvent.setEventType(TenantSubscriptionEventType.PAYMENT_REFUNDED);
        refundEvent.setOldPlan(event.getOldPlan());
        refundEvent.setNewPlan(event.getNewPlan());
        refundEvent.setOldStatus(event.getTenantSubscription() != null ? event.getTenantSubscription().getStatus() : event.getNewStatus());
        refundEvent.setNewStatus(event.getTenantSubscription() != null ? event.getTenantSubscription().getStatus() : event.getNewStatus());
        refundEvent.setActorUserId(actorUserId);
        refundEvent.setOccurredAt(Instant.now());
        eventRepository.save(refundEvent);
    }

    @Transactional
    public void assignPriest(Long priestId, UUID tenantId, UUID actorUserId) {
        PriestEntity priest = priestRepository.findById(priestId)
                .orElseThrow(() -> new EntityNotFoundException("Priest not found"));
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
        supportAccessService.authorizeCurrentRequestSession(actorUserId, tenantId, SupportAccessScope.READ_WRITE);
        priest.setTenant(tenant);
        priest.setStatus(PriestStatus.ACTIVE);
        priest.setActive(true);
        tenant.setUpdatedBy(actorUserId);
        priestRepository.save(priest);
    }

    @Transactional
    public void updatePriestStatus(Long priestId, PriestStatus status, UUID actorUserId) {
        PriestEntity priest = priestRepository.findById(priestId)
                .orElseThrow(() -> new EntityNotFoundException("Priest not found"));
        if (priest.getTenant() != null) {
            supportAccessService.authorizeCurrentRequestSession(actorUserId, priest.getTenant().getId(), SupportAccessScope.READ_WRITE);
        }
        priest.setStatus(status);
        if (status == PriestStatus.ACTIVE) {
            priest.setActive(true);
        } else if (status == PriestStatus.INACTIVE) {
            priest.setActive(false);
        }
        priestRepository.save(priest);
    }
}
