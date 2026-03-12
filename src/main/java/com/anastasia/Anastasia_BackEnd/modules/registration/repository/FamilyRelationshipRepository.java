package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyRelationshipRepository extends JpaRepository<FamilyRelationshipEntity, Long> {
    long countByOwnerMemberIdAndTenantId(Long ownerMemberId, UUID tenantId);

    boolean existsByOwnerMemberIdAndTenantIdAndRelationshipTypeAndRelatedMemberId(
            Long ownerMemberId,
            UUID tenantId,
            com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipType relationshipType,
            Long relatedMemberId
    );

    boolean existsByOwnerMemberIdAndTenantIdAndRelationshipTypeAndRelatedChildId(
            Long ownerMemberId,
            UUID tenantId,
            com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipType relationshipType,
            Long relatedChildId
    );

    List<FamilyRelationshipEntity> findByOwnerMemberIdAndTenantIdOrderByRelationshipTypeAscSortOrderAscIdAsc(
            Long ownerMemberId,
            UUID tenantId
    );

    java.util.Optional<FamilyRelationshipEntity> findByIdAndOwnerMemberIdAndTenantId(Long id, Long ownerMemberId, UUID tenantId);
}
