package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.core.auth.repository.TokenRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Account;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Fund;
import com.anastasia.Anastasia_BackEnd.modules.accounting.model.Transaction;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.AccountRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.FundRepository;
import com.anastasia.Anastasia_BackEnd.modules.accounting.repository.TransactionRepository;
import com.anastasia.Anastasia_BackEnd.modules.appointments.model.AppointmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.appointments.repository.AppointmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryEntity;
import com.anastasia.Anastasia_BackEnd.modules.calendar.repository.CalendarEntryRepository;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventEntity;
import com.anastasia.Anastasia_BackEnd.modules.events.repository.EventRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupJoinRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.GroupRepository;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupEntity;
import com.anastasia.Anastasia_BackEnd.modules.groups.model.GroupJoinRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentIntent;
import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentSubscription;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentIntentRepository;
import com.anastasia.Anastasia_BackEnd.modules.payments.repository.PaymentSubscriptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardAuditEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardTemplateEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.PromoRedemptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntitlementAuditEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeatureOverrideEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantPlanGrantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEventEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WebhookEventReceiptEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.WorkspaceInitializationMode;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.FamilyRelationshipRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberTransferRequestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MembershipCardTemplateRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PromoRedemptionRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantEntitlementAuditRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantFeatureOverrideRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantPlanGrantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantSubscriptionEventRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.WebhookEventReceiptRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantDemoTemplateCloneService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.onboarding.TenantDemoWorkspaceSeederService;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEntity;
import com.anastasia.Anastasia_BackEnd.modules.staff.repository.StaffRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.TenantUserPermissionGrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantWorkspaceLifecycleService {

    private final TenantRepository tenantRepository;
    private final ChurchRepository churchRepository;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final TenantUserPermissionGrantRepository tenantUserPermissionGrantRepository;
    private final PriestRepository priestRepository;
    private final StaffRepository staffRepository;
    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final FamilyRelationshipRepository familyRelationshipRepository;
    private final MembershipCardAuditRepository membershipCardAuditRepository;
    private final MembershipCardRepository membershipCardRepository;
    private final MembershipCardTemplateRepository membershipCardTemplateRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupRepository groupRepository;
    private final EventRepository eventRepository;
    private final CalendarEntryRepository calendarEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final TenantPlanGrantRepository tenantPlanGrantRepository;
    private final TenantFeatureOverrideRepository tenantFeatureOverrideRepository;
    private final PromoRedemptionRepository promoRedemptionRepository;
    private final TenantSubscriptionEventRepository tenantSubscriptionEventRepository;
    private final TenantEntitlementAuditRepository tenantEntitlementAuditRepository;
    private final WebhookEventReceiptRepository webhookEventReceiptRepository;
    private final MemberTransferRequestRepository memberTransferRequestRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentSubscriptionRepository paymentSubscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final FundRepository fundRepository;
    private final AccountRepository accountRepository;
    private final TenantDemoTemplateCloneService tenantDemoTemplateCloneService;
    private final TenantDemoWorkspaceSeederService tenantDemoWorkspaceSeederService;

    @Value("${app.tenants.demo.grace-period-days:14}")
    private long demoGracePeriodDays;

    @Value("${app.tenants.paid.archive-after-days:180}")
    private long paidArchiveAfterDays;

    @Value("${app.tenants.retention.warning-days:7}")
    private long retentionWarningDays;

    @Transactional
    public TenantEntity syncTenantLifecycle(UUID tenantId, UUID actorUserId) {
        TenantEntity tenant = tenantRepository.findWithSubscriptionById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        syncTenantLifecycle(tenant, actorUserId, Instant.now());
        return tenantRepository.save(tenant);
    }

    @Transactional
    public TenantEntity resetDemoWorkspace(UUID tenantId, UUID actorUserId) {
        TenantEntity tenant = tenantRepository.findWithSubscriptionById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (!tenant.isDemoWorkspace()) {
            throw new IllegalStateException("Only demo workspaces can be reset.");
        }
        if (hasEverPaid(tenant.getSubscription())) {
            throw new IllegalStateException("Paid workspaces cannot be reset from the demo template.");
        }
        if (tenant.getDeletedAt() != null || tenant.getPurgedAt() != null) {
            throw new IllegalStateException("Purged workspaces cannot be reset.");
        }

        UserEntity owner = resolveWorkspaceOwner(tenant, actorUserId);
        clearWorkspaceData(tenant, owner);

        boolean cloned = tenantDemoTemplateCloneService.cloneWorkspaceFromConfiguredTemplate(tenant, owner);
        if (!cloned) {
            tenantDemoWorkspaceSeederService.seedDemoWorkspace(tenant, owner);
        }

        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setSuspendedAt(null);
        tenant.setDeactivatedAt(null);
        tenant.setClosedAt(null);
        tenant.setScheduledPurgeAt(null);
        tenant.setPurgedAt(null);
        tenant.setArchivedAt(null);
        tenant.setArchiveScheduledAt(null);
        tenant.setDeletedAt(null);
        tenant.setSuspensionReason(null);

        TenantSubscriptionEntity subscription = tenant.getSubscription();
        if (subscription != null && subscription.getCurrentPeriodEndAt() != null) {
            subscription.setGracePeriodEndsAt(subscription.getCurrentPeriodEndAt().plus(Duration.ofDays(demoGracePeriodDays)));
        }
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public boolean isRetentionWarningActive(TenantEntity tenant, TenantSubscriptionEntity subscription, Instant now) {
        Instant threshold = null;
        if (tenant.getScheduledPurgeAt() != null) {
            threshold = tenant.getScheduledPurgeAt();
        } else if (tenant.getArchiveScheduledAt() != null) {
            threshold = tenant.getArchiveScheduledAt();
        } else if (subscription != null && effectiveAccessDeadline(subscription) != null) {
            threshold = effectiveAccessDeadline(subscription);
        }
        return threshold != null && !threshold.isAfter(now.plus(Duration.ofDays(retentionWarningDays)));
    }

    @Scheduled(cron = "${app.tenants.lifecycle.sync-cron:0 15 * * * *}")
    @Transactional
    public void processRetentionPolicies() {
        Instant now = Instant.now();

        tenantRepository.findActiveDemoWorkspaces()
                .forEach(tenant -> syncTenantLifecycle(tenant, null, now));

        tenantRepository.findDemoWorkspacesDueForPurge(now)
                .forEach(tenant -> purgeDemoWorkspace(tenant, now));

        tenantRepository.findTenantsDueForArchive(now)
                .forEach(tenant -> archiveTenantWorkspace(tenant, now));
    }

    private void syncTenantLifecycle(TenantEntity tenant, UUID actorUserId, Instant now) {
        TenantSubscriptionEntity subscription = tenant.getSubscription();
        if (subscription == null || tenant.getDeletedAt() != null) {
            return;
        }

        if (tenant.isDemoWorkspace() && !hasEverPaid(subscription)) {
            syncDemoWorkspaceLifecycle(tenant, subscription, now);
            return;
        }

        syncPaidWorkspaceLifecycle(tenant, subscription, now);
    }

    private void syncDemoWorkspaceLifecycle(TenantEntity tenant, TenantSubscriptionEntity subscription, Instant now) {
        Instant accessDeadline = effectiveAccessDeadline(subscription);
        if (hasActiveAccess(subscription, now)) {
            tenant.setScheduledPurgeAt(null);
            subscription.setGracePeriodEndsAt(null);
            if (tenant.getStatus() == TenantStatus.SUSPENDED && tenant.getPurgedAt() == null) {
                tenant.setStatus(TenantStatus.ACTIVE);
                tenant.setSuspendedAt(null);
                tenant.setSuspensionReason(null);
            }
            return;
        }

        if (accessDeadline == null) {
            return;
        }

        Instant purgeAt = accessDeadline.plus(Duration.ofDays(demoGracePeriodDays));
        tenant.setScheduledPurgeAt(purgeAt);
        subscription.setGracePeriodEndsAt(purgeAt);

        if (tenant.getPurgedAt() == null) {
            tenant.setStatus(TenantStatus.SUSPENDED);
            if (tenant.getSuspendedAt() == null) {
                tenant.setSuspendedAt(now);
            }
            tenant.setSuspensionReason("Demo trial expired");
        }
    }

    private void syncPaidWorkspaceLifecycle(TenantEntity tenant, TenantSubscriptionEntity subscription, Instant now) {
        if (!shouldArchiveOnLapse(subscription)) {
            if (hasActiveAccess(subscription, now)) {
                tenant.setArchiveScheduledAt(null);
                if (tenant.getArchivedAt() == null && tenant.getStatus() == TenantStatus.SUSPENDED) {
                    tenant.setStatus(TenantStatus.ACTIVE);
                    tenant.setSuspendedAt(null);
                    tenant.setSuspensionReason(null);
                }
            }
            return;
        }

        Instant accessDeadline = effectiveAccessDeadline(subscription);
        if (accessDeadline == null || accessDeadline.isAfter(now)) {
            return;
        }

        if (tenant.getArchiveScheduledAt() == null) {
            tenant.setArchiveScheduledAt(accessDeadline.plus(Duration.ofDays(paidArchiveAfterDays)));
        }

        if (tenant.getStatus() == TenantStatus.ACTIVE) {
            tenant.setStatus(TenantStatus.SUSPENDED);
            tenant.setSuspendedAt(now);
            tenant.setSuspensionReason("Subscription access lapsed");
        }
    }

    private void purgeDemoWorkspace(TenantEntity tenant, Instant now) {
        if (tenant.getDeletedAt() != null || tenant.getPurgedAt() != null) {
            return;
        }

        UserEntity owner = resolveWorkspaceOwner(tenant, null);
        clearWorkspaceData(tenant, owner);
        deactivateTenantUsers(tenant, now);

        tenant.setStatus(TenantStatus.CLOSED);
        tenant.setDeactivatedAt(now);
        tenant.setClosedAt(now);
        tenant.setDeletedAt(now);
        tenant.setPurgedAt(now);
        tenant.setSuspensionReason("Demo trial data purged after grace period");
        purgeTenantGovernanceData(tenant.getId());
        tenantRepository.save(tenant);
        log.info("Purged demo workspace for tenant {}", tenant.getId());
    }

    private void archiveTenantWorkspace(TenantEntity tenant, Instant now) {
        if (tenant.getDeletedAt() != null || tenant.getArchivedAt() != null) {
            return;
        }

        deactivateTenantUsers(tenant, now);
        tenant.setStatus(TenantStatus.CLOSED);
        tenant.setSuspendedAt(now);
        tenant.setClosedAt(now);
        tenant.setArchivedAt(now);
        tenant.setSuspensionReason("Workspace archived after subscription lapse");
        tenantRepository.save(tenant);
        log.info("Archived tenant workspace {}", tenant.getId());
    }

    private void clearWorkspaceData(TenantEntity tenant, UserEntity owner) {
        UUID tenantId = tenant.getId();

        List<MembershipCardAuditEntity> cardAudits = membershipCardAuditRepository.findByTenantId(tenantId);
        if (!cardAudits.isEmpty()) {
            membershipCardAuditRepository.deleteAllInBatch(cardAudits);
        }

        List<PaymentIntent> paymentIntents = paymentIntentRepository.findByTenantId(tenantId);
        if (!paymentIntents.isEmpty()) {
            paymentIntentRepository.deleteAllInBatch(paymentIntents);
        }

        List<PaymentSubscription> paymentSubscriptions = paymentSubscriptionRepository.findByTenantId(tenantId);
        if (!paymentSubscriptions.isEmpty()) {
            paymentSubscriptionRepository.deleteAllInBatch(paymentSubscriptions);
        }

        List<Transaction> transactions = transactionRepository.findByTenantId(tenantId);
        if (!transactions.isEmpty()) {
            transactionRepository.deleteAllInBatch(transactions);
        }

        List<Fund> funds = fundRepository.findByTenantId(tenantId);
        if (!funds.isEmpty()) {
            fundRepository.deleteAllInBatch(funds);
        }

        List<Account> accounts = accountRepository.findByTenantId(tenantId);
        if (!accounts.isEmpty()) {
            accountRepository.deleteAllInBatch(accounts);
        }

        List<MembershipCardEntity> cards = membershipCardRepository.findByTenantId(tenantId);
        if (!cards.isEmpty()) {
            membershipCardRepository.deleteAllInBatch(cards);
        }

        List<MembershipCardTemplateEntity> cardTemplates = membershipCardTemplateRepository.findByTenantId(tenantId);
        if (!cardTemplates.isEmpty()) {
            membershipCardTemplateRepository.deleteAllInBatch(cardTemplates);
        }

        List<FamilyRelationshipEntity> familyRelationships = familyRelationshipRepository.findByTenantId(tenantId);
        if (!familyRelationships.isEmpty()) {
            familyRelationshipRepository.deleteAllInBatch(familyRelationships);
        }

        List<AppointmentEntity> appointments = appointmentRepository.findByTenantIdOrderByStartAtUtcAsc(tenantId);
        if (!appointments.isEmpty()) {
            appointmentRepository.deleteAllInBatch(appointments);
        }

        List<CalendarEntryEntity> calendarEntries = calendarEntryRepository.findByTenantIdOrderByStartAtUtcAsc(tenantId);
        if (!calendarEntries.isEmpty()) {
            calendarEntryRepository.deleteAllInBatch(calendarEntries);
        }

        List<EventEntity> events = eventRepository.findByTenantId(tenantId);
        if (!events.isEmpty()) {
            eventRepository.deleteAllInBatch(events);
        }

        List<GroupJoinRequestEntity> joinRequests = groupJoinRequestRepository.findByTenantId(tenantId);
        if (!joinRequests.isEmpty()) {
            groupJoinRequestRepository.deleteAllInBatch(joinRequests);
        }

        List<GroupEntity> groups = groupRepository.findByTenantId(tenantId);
        if (!groups.isEmpty()) {
            groups.forEach(group -> {
                group.getManagers().clear();
                group.getUsers().clear();
            });
            groupRepository.saveAll(groups);
            groupRepository.deleteAllInBatch(groups);
        }

        List<Child_MemberEntity> children = childRepository.findByTenantId(tenantId);
        if (!children.isEmpty()) {
            childRepository.deleteAllInBatch(children);
        }

        List<Adult_MemberEntity> adults = memberRepository.findByTenantId(tenantId);
        if (!adults.isEmpty()) {
            memberRepository.deleteAllInBatch(adults);
        }

        List<StaffEntity> staffProfiles = new ArrayList<>(staffRepository.findByTenant_Id(tenantId));
        if (!staffProfiles.isEmpty()) {
            staffProfiles.forEach(staff -> staff.setReportsTo(null));
            staffRepository.saveAll(staffProfiles);
            staffRepository.deleteAllInBatch(staffProfiles);
        }

        churchRepository.findByTenantId(tenantId).ifPresent(church -> {
            List<PriestEntity> priests = priestRepository.findByChurch_ChurchId(church.getChurchId());
            if (!priests.isEmpty()) {
                priestRepository.deleteAllInBatch(priests);
            }
        });

        Set<UUID> preservedUserIds = new HashSet<>();
        tenantAdminAssignmentRepository.findByTenant_IdOrderByCreatedAtAsc(tenantId)
                .forEach(assignment -> preservedUserIds.add(assignment.getUserId()));
        if (owner != null && owner.getUuid() != null) {
            preservedUserIds.add(owner.getUuid());
        }

        List<UserEntity> tenantUsers = userRepository.findByAffiliatedTenantId(tenantId);
        List<UserEntity> removableUsers = tenantUsers.stream()
                .filter(user -> user.getUuid() != null && !preservedUserIds.contains(user.getUuid()))
                .toList();
        removableUsers.forEach(user -> tokenRepository.revokeAllActiveTokensByUserUuid(user.getUuid(), Instant.now()));
        removableUsers.forEach(user -> tenantUserPermissionGrantRepository.deleteByUserIdAndTenantId(user.getUuid(), tenantId));
        if (!removableUsers.isEmpty()) {
            userRepository.deleteAllInBatch(removableUsers);
        }
    }

    private void purgeTenantGovernanceData(UUID tenantId) {
        List<TenantPlanGrantEntity> planGrants = tenantPlanGrantRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
        if (!planGrants.isEmpty()) {
            tenantPlanGrantRepository.deleteAllInBatch(planGrants);
        }

        List<TenantFeatureOverrideEntity> featureOverrides = tenantFeatureOverrideRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
        if (!featureOverrides.isEmpty()) {
            tenantFeatureOverrideRepository.deleteAllInBatch(featureOverrides);
        }

        List<PromoRedemptionEntity> promoRedemptions = promoRedemptionRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId);
        if (!promoRedemptions.isEmpty()) {
            promoRedemptionRepository.deleteAllInBatch(promoRedemptions);
        }

        List<TenantSubscriptionEventEntity> subscriptionEvents = tenantSubscriptionEventRepository.findByTenantIdOrderByOccurredAtDesc(tenantId);
        if (!subscriptionEvents.isEmpty()) {
            tenantSubscriptionEventRepository.deleteAllInBatch(subscriptionEvents);
        }

        List<TenantEntitlementAuditEntity> entitlementAudits = tenantEntitlementAuditRepository.findByTenant_IdOrderByOccurredAtDesc(tenantId);
        if (!entitlementAudits.isEmpty()) {
            tenantEntitlementAuditRepository.deleteAllInBatch(entitlementAudits);
        }

        List<WebhookEventReceiptEntity> webhookReceipts = webhookEventReceiptRepository.findByTenant_Id(tenantId);
        if (!webhookReceipts.isEmpty()) {
            webhookEventReceiptRepository.deleteAllInBatch(webhookReceipts);
        }

        List<MemberTransferRequestEntity> transferRequests = memberTransferRequestRepository.findByFromTenant_IdOrToTenant_Id(tenantId, tenantId);
        if (!transferRequests.isEmpty()) {
            memberTransferRequestRepository.deleteAllInBatch(transferRequests);
        }

        List<TenantAdminAssignmentEntity> adminAssignments = tenantAdminAssignmentRepository.findByTenant_IdOrderByCreatedAtAsc(tenantId);
        if (!adminAssignments.isEmpty()) {
            tenantAdminAssignmentRepository.deleteAllInBatch(adminAssignments);
        }
    }

    private void deactivateTenantUsers(TenantEntity tenant, Instant now) {
        List<UserEntity> users = userRepository.findByAffiliatedTenantId(tenant.getId());
        users.forEach(user -> {
            tokenRepository.revokeAllActiveTokensByUserUuid(user.getUuid(), now);
            user.setStatus(UserStatus.DISABLED);
            user.setDeletedAt(now);
            user.setLockedAt(now);
        });
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
        }
    }

    private UserEntity resolveWorkspaceOwner(TenantEntity tenant, UUID preferredUserId) {
        if (preferredUserId != null) {
            UserEntity preferred = userRepository.findById(preferredUserId).orElse(null);
            if (preferred != null) {
                return preferred;
            }
        }
        UserEntity tenantAdmin = userRepository.findTenantAdmin(tenant.getId()).orElse(null);
        if (tenantAdmin != null) {
            return tenantAdmin;
        }
        return userRepository.findByEmail(tenant.getOwnerEmail()).orElseThrow(() -> new IllegalStateException("Tenant owner not found"));
    }

    private boolean hasActiveAccess(TenantSubscriptionEntity subscription, Instant now) {
        if (subscription == null || subscription.getStatus() == null) {
            return false;
        }
        return switch (subscription.getStatus()) {
            case ACTIVE -> true;
            case TRIALING -> {
                Instant deadline = effectiveAccessDeadline(subscription);
                yield deadline == null || !deadline.isBefore(now);
            }
            case PAST_DUE, CANCELED, SUSPENDED -> false;
        };
    }

    private Instant effectiveAccessDeadline(TenantSubscriptionEntity subscription) {
        if (subscription == null) {
            return null;
        }
        if (subscription.getTrialEndAt() != null) {
            return subscription.getTrialEndAt();
        }
        if (subscription.getCurrentPeriodEndAt() != null) {
            return subscription.getCurrentPeriodEndAt();
        }
        if (subscription.getEndedAt() != null) {
            return subscription.getEndedAt();
        }
        return subscription.getCanceledAt();
    }

    private boolean shouldArchiveOnLapse(TenantSubscriptionEntity subscription) {
        return subscription != null
                && hasEverPaid(subscription)
                && (subscription.getStatus() == SubscriptionStatus.CANCELED
                || subscription.getStatus() == SubscriptionStatus.PAST_DUE
                || subscription.getStatus() == SubscriptionStatus.SUSPENDED);
    }

    private boolean hasEverPaid(TenantSubscriptionEntity subscription) {
        return subscription != null
                && (subscription.getLastPaymentAt() != null
                || subscription.getProviderLinks() != null && !subscription.getProviderLinks().isEmpty());
    }
}
