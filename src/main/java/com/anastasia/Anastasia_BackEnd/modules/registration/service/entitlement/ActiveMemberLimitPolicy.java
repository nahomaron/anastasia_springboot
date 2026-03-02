package com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement;

import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChildRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActiveMemberLimitPolicy {

    private static final List<String> ACTIVE_STATUSES = List.of("APPROVED", "ACTIVE");

    private final MemberRepository memberRepository;
    private final ChildRepository childRepository;
    private final TenantEntitlementAccessService entitlementAccessService;

    public void assertCanActivateMembers(UUID tenantId, int additionalCount) {
        if (additionalCount <= 0) {
            return;
        }
        int limit = entitlementAccessService.activeMembersLimit(tenantId);
        if (limit <= 0) {
            throw new AccessDeniedException("Active member limit is not configured for current tenant plan.");
        }

        long currentActive = memberRepository.countByTenantIdAndStatusIn(tenantId, ACTIVE_STATUSES)
                + childRepository.countByTenantIdAndStatusIn(tenantId, ACTIVE_STATUSES);
        if (currentActive + additionalCount > limit) {
            throw new AccessDeniedException(
                    "Active member limit exceeded for current tenant plan. " +
                            "Current: " + currentActive + ", limit: " + limit
            );
        }
    }
}
