package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantAdminAssignmentServiceImpl implements TenantAdminAssignmentService {

    private static final EnumSet<MembershipStatus> PRIMARY_OWNER_GUARDED_STATUSES =
            EnumSet.of(MembershipStatus.ACTIVE, MembershipStatus.INVITED);

    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final LocalizedMessageService messageService;

    @Override
    @Transactional
    public TenantAdminAssignmentEntity inviteUserToTenant(UUID tenantId, UUID userId, TenantRole role, UUID actorUserId) {
        TenantEntity tenant = requireTenant(tenantId);
        UserEntity user = requireUser(userId);
        if (user.getTenantId() != null && !tenantId.equals(user.getTenantId())) {
            throw new IllegalStateException(messageService.get(
                    "tenant.membership.userBelongsToAnotherTenant",
                    "User belongs to another tenant"
            ));
        }

        TenantRole resolvedRole = role != null ? role : TenantRole.COMMITTEE;

        TenantAdminAssignmentEntity entity = tenantAdminAssignmentRepository.findByTenant_IdAndUserId(tenantId, userId)
                .orElseGet(() -> TenantAdminAssignmentEntity.builder()
                        .tenant(tenant)
                        .userId(userId)
                        .createdByUserId(actorUserId)
                        .build());

        ensurePrimaryOwnerConstraint(tenantId, resolvedRole, entity);

        entity.setRole(resolvedRole);
        entity.setStatus(MembershipStatus.INVITED);
        entity.setUpdatedByUserId(actorUserId);

        if (entity.getCreatedByUserId() == null) {
            entity.setCreatedByUserId(actorUserId);
        }

        return tenantAdminAssignmentRepository.save(entity);
    }

    @Override
    @Transactional
    public TenantAdminAssignmentEntity activateMembership(UUID tenantId, UUID userId, UUID actorUserId) {
        TenantAdminAssignmentEntity entity = requireTenantAdminAssignment(tenantId, userId);
        ensurePrimaryOwnerConstraint(tenantId, entity.getRole(), entity);
        ensureNoOtherActiveMembership(userId, tenantId);
        entity.setStatus(MembershipStatus.ACTIVE);
        entity.setUpdatedByUserId(actorUserId);
        TenantAdminAssignmentEntity saved = tenantAdminAssignmentRepository.save(entity);

        UserEntity user = requireUser(userId);
        user.assignAffiliatedTenant(saved.getTenant());
        userRepository.save(user);
        return saved;
    }

    @Override
    @Transactional
    public TenantAdminAssignmentEntity changeRole(UUID tenantId, UUID userId, TenantRole newRole, UUID actorUserId) {
        if (newRole == null) {
            throw new IllegalArgumentException(messageService.get(
                    "tenant.membership.newRole.required",
                    "New role is required"
            ));
        }

        TenantAdminAssignmentEntity entity = requireTenantAdminAssignment(tenantId, userId);
        ensurePrimaryOwnerConstraint(tenantId, newRole, entity);

        entity.setRole(newRole);
        entity.setUpdatedByUserId(actorUserId);
        return tenantAdminAssignmentRepository.save(entity);
    }

    @Override
    @Transactional
    public TenantAdminAssignmentEntity setBillingContact(UUID tenantId, UUID userId, boolean billingContact, UUID actorUserId) {
        TenantAdminAssignmentEntity entity = requireTenantAdminAssignment(tenantId, userId);
        if (entity.getStatus() == MembershipStatus.REMOVED) {
            throw new IllegalStateException(messageService.get(
                    "tenant.membership.billingContact.removedUserForbidden",
                    "Removed tenant users cannot be billing contacts"
            ));
        }

        entity.setBillingContact(billingContact);
        entity.setUpdatedByUserId(actorUserId);
        return tenantAdminAssignmentRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantAdminAssignmentEntity> listMembers(UUID tenantId) {
        requireTenant(tenantId);
        return tenantAdminAssignmentRepository.findByTenant_IdOrderByCreatedAtAsc(tenantId);
    }

    private TenantEntity requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.notFound",
                        "Tenant not found"
                )));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "user.notFound",
                        "User not found"
                )));
    }

    private TenantAdminAssignmentEntity requireTenantAdminAssignment(UUID tenantId, UUID userId) {
        return tenantAdminAssignmentRepository.findByTenant_IdAndUserId(tenantId, userId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "tenant.membership.assignment.notFound",
                        "Tenant admin assignment not found"
                )));
    }

    private void ensurePrimaryOwnerConstraint(UUID tenantId, TenantRole targetRole, TenantAdminAssignmentEntity currentEntity) {
        if (targetRole != TenantRole.PRIMARY_OWNER) {
            return;
        }

        long activeCount = tenantAdminAssignmentRepository.countByTenantIdAndRoleAndStatus(
                tenantId,
                TenantRole.PRIMARY_OWNER,
                MembershipStatus.ACTIVE
        );
        long invitedCount = tenantAdminAssignmentRepository.countByTenantIdAndRoleAndStatus(
                tenantId,
                TenantRole.PRIMARY_OWNER,
                MembershipStatus.INVITED
        );

        long existingPrimaryOwners = activeCount + invitedCount;
        boolean currentIsAlreadyPrimaryOwner = currentEntity.getId() != null
                && currentEntity.getRole() == TenantRole.PRIMARY_OWNER
                && PRIMARY_OWNER_GUARDED_STATUSES.contains(currentEntity.getStatus());

        if (!currentIsAlreadyPrimaryOwner && existingPrimaryOwners > 0) {
            throw new IllegalStateException(messageService.get(
                    "tenant.membership.primaryOwner.alreadyExists",
                    "Tenant already has a PRIMARY_OWNER"
            ));
        }
    }

    private void ensureNoOtherActiveMembership(UUID userId, UUID tenantId) {
        List<TenantAdminAssignmentEntity> otherActiveMemberships =
                tenantAdminAssignmentRepository.findByUserIdAndStatusAndTenant_IdNot(userId, MembershipStatus.ACTIVE, tenantId);
        if (!otherActiveMemberships.isEmpty()) {
            throw new IllegalStateException(messageService.get(
                    "tenant.membership.activeElsewhere",
                    "User already has an active membership in another tenant"
            ));
        }
    }
}
