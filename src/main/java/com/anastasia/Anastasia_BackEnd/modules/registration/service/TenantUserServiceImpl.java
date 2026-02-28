package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantUserEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantUserRepository;
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
public class TenantUserServiceImpl implements TenantUserService {

    private static final EnumSet<MembershipStatus> PRIMARY_OWNER_GUARDED_STATUSES =
            EnumSet.of(MembershipStatus.ACTIVE, MembershipStatus.INVITED);

    private final TenantUserRepository tenantUserRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TenantUserEntity inviteUserToTenant(UUID tenantId, UUID userId, TenantRole role, UUID actorUserId) {
        TenantEntity tenant = requireTenant(tenantId);
        UserEntity user = requireUser(userId);
        if (user.getTenantId() != null && !tenantId.equals(user.getTenantId())) {
            throw new IllegalStateException("User belongs to another tenant");
        }

        TenantRole resolvedRole = role != null ? role : TenantRole.COMMITTEE;

        TenantUserEntity entity = tenantUserRepository.findByTenant_IdAndUserId(tenantId, userId)
                .orElseGet(() -> TenantUserEntity.builder()
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

        return tenantUserRepository.save(entity);
    }

    @Override
    @Transactional
    public TenantUserEntity activateMembership(UUID tenantId, UUID userId, UUID actorUserId) {
        TenantUserEntity entity = requireTenantUser(tenantId, userId);
        ensurePrimaryOwnerConstraint(tenantId, entity.getRole(), entity);
        entity.setStatus(MembershipStatus.ACTIVE);
        entity.setUpdatedByUserId(actorUserId);
        return tenantUserRepository.save(entity);
    }

    @Override
    @Transactional
    public TenantUserEntity changeRole(UUID tenantId, UUID userId, TenantRole newRole, UUID actorUserId) {
        if (newRole == null) {
            throw new IllegalArgumentException("New role is required");
        }

        TenantUserEntity entity = requireTenantUser(tenantId, userId);
        ensurePrimaryOwnerConstraint(tenantId, newRole, entity);

        entity.setRole(newRole);
        entity.setUpdatedByUserId(actorUserId);
        return tenantUserRepository.save(entity);
    }

    @Override
    @Transactional
    public TenantUserEntity setBillingContact(UUID tenantId, UUID userId, boolean billingContact, UUID actorUserId) {
        TenantUserEntity entity = requireTenantUser(tenantId, userId);
        if (entity.getStatus() == MembershipStatus.REMOVED) {
            throw new IllegalStateException("Removed tenant users cannot be billing contacts");
        }

        entity.setBillingContact(billingContact);
        entity.setUpdatedByUserId(actorUserId);
        return tenantUserRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantUserEntity> listMembers(UUID tenantId) {
        requireTenant(tenantId);
        return tenantUserRepository.findByTenant_IdOrderByCreatedAtAsc(tenantId);
    }

    private TenantEntity requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private TenantUserEntity requireTenantUser(UUID tenantId, UUID userId) {
        return tenantUserRepository.findByTenant_IdAndUserId(tenantId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant user relationship not found"));
    }

    private void ensurePrimaryOwnerConstraint(UUID tenantId, TenantRole targetRole, TenantUserEntity currentEntity) {
        if (targetRole != TenantRole.PRIMARY_OWNER) {
            return;
        }

        long activeCount = tenantUserRepository.countByTenantIdAndRoleAndStatus(
                tenantId,
                TenantRole.PRIMARY_OWNER,
                MembershipStatus.ACTIVE
        );
        long invitedCount = tenantUserRepository.countByTenantIdAndRoleAndStatus(
                tenantId,
                TenantRole.PRIMARY_OWNER,
                MembershipStatus.INVITED
        );

        long existingPrimaryOwners = activeCount + invitedCount;
        boolean currentIsAlreadyPrimaryOwner = currentEntity.getId() != null
                && currentEntity.getRole() == TenantRole.PRIMARY_OWNER
                && PRIMARY_OWNER_GUARDED_STATUSES.contains(currentEntity.getStatus());

        if (!currentIsAlreadyPrimaryOwner && existingPrimaryOwners > 0) {
            throw new IllegalStateException("Tenant already has a PRIMARY_OWNER");
        }
    }
}
