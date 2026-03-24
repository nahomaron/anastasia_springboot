package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipCardAuditRepository extends JpaRepository<MembershipCardAuditEntity, Long> {
    List<MembershipCardAuditEntity> findByTenantId(UUID tenantId);
}
