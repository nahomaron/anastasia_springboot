package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipCardTemplateRepository extends JpaRepository<MembershipCardTemplateEntity, Long> {
    List<MembershipCardTemplateEntity> findByTenantId(UUID tenantId);
    List<MembershipCardTemplateEntity> findByTenantIdAndActiveTrueOrderBySortOrderAscDisplayNameAsc(UUID tenantId);
    Optional<MembershipCardTemplateEntity> findByTenantIdAndIsDefaultTrue(UUID tenantId);
    Optional<MembershipCardTemplateEntity> findByIdAndTenantId(Long id, UUID tenantId);
    boolean existsByTenantId(UUID tenantId);
}
