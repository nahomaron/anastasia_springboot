package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;

import java.util.List;
import java.util.UUID;

public interface TenantAdminAssignmentService {

    /**
     * Creates or updates an INVITED tenant-user assignment for a user.
     */
    TenantAdminAssignmentEntity inviteUserToTenant(UUID tenantId, UUID userId, TenantRole role, UUID actorUserId);

    /**
     * Activates an existing tenant-user relationship when an invitation is accepted.
     */
    TenantAdminAssignmentEntity activateMembership(UUID tenantId, UUID userId, UUID actorUserId);

    /**
     * Changes tenant user role with ownership governance checks.
     */
    TenantAdminAssignmentEntity changeRole(UUID tenantId, UUID userId, TenantRole newRole, UUID actorUserId);

    /**
     * Sets or clears billing contact for a tenant user.
     */
    TenantAdminAssignmentEntity setBillingContact(UUID tenantId, UUID userId, boolean billingContact, UUID actorUserId);

    /**
     * Lists all tenant user rows for a tenant.
     */
    List<TenantAdminAssignmentEntity> listMembers(UUID tenantId);
}
