package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantUserEntity;

import java.util.List;
import java.util.UUID;

public interface TenantUserService {

    /**
     * Creates or updates an INVITED tenant-user assignment for a user.
     */
    TenantUserEntity inviteUserToTenant(UUID tenantId, UUID userId, TenantRole role, UUID actorUserId);

    /**
     * Activates an existing tenant-user relationship when an invitation is accepted.
     */
    TenantUserEntity activateMembership(UUID tenantId, UUID userId, UUID actorUserId);

    /**
     * Changes tenant user role with ownership governance checks.
     */
    TenantUserEntity changeRole(UUID tenantId, UUID userId, TenantRole newRole, UUID actorUserId);

    /**
     * Sets or clears billing contact for a tenant user.
     */
    TenantUserEntity setBillingContact(UUID tenantId, UUID userId, boolean billingContact, UUID actorUserId);

    /**
     * Lists all tenant user rows for a tenant.
     */
    List<TenantUserEntity> listMembers(UUID tenantId);
}
