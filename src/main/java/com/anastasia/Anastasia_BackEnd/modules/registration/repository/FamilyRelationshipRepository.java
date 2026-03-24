package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyRelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FamilyRelationshipRepository extends JpaRepository<FamilyRelationshipEntity, Long> {
    long countByOwnerMemberIdAndTenantId(Long ownerMemberId, UUID tenantId);

    boolean existsByOwnerMemberIdAndTenantIdAndRelationshipTypeAndRelatedMemberId(
            Long ownerMemberId,
            UUID tenantId,
            FamilyRelationshipType relationshipType,
            Long relatedMemberId
    );

    boolean existsByOwnerMemberIdAndTenantIdAndRelationshipTypeAndRelatedChildId(
            Long ownerMemberId,
            UUID tenantId,
            FamilyRelationshipType relationshipType,
            Long relatedChildId
    );

    List<FamilyRelationshipEntity> findByOwnerMemberIdAndTenantIdOrderByRelationshipTypeAscSortOrderAscIdAsc(
            Long ownerMemberId,
            UUID tenantId
    );

    Optional<FamilyRelationshipEntity> findByIdAndOwnerMemberIdAndTenantId(Long id, Long ownerMemberId, UUID tenantId);

    List<FamilyRelationshipEntity> findByTenantId(UUID tenantId);

    @Query("""
            SELECT fr.ownerMember.id as ownerMemberId,
                   fr.relatedChild.id as childId,
                   CASE WHEN fr.relatedChild.father.id = fr.ownerMember.id THEN true ELSE false END as father,
                   CASE WHEN fr.relatedChild.mother.id = fr.ownerMember.id THEN true ELSE false END as mother
            FROM FamilyRelationshipEntity fr
            WHERE fr.tenantId = :tenantId
              AND fr.ownerMember.id IN :ownerIds
              AND fr.relationshipType = :relationshipType
              AND fr.relatedChild IS NOT NULL
            """)
    List<OwnerChildRelationship> findChildRelationshipsByOwnerIdsAndTenantIdAndRelationshipType(
            @Param("ownerIds") Set<Long> ownerIds,
            @Param("tenantId") UUID tenantId,
            @Param("relationshipType") FamilyRelationshipType relationshipType
    );

    interface OwnerChildRelationship {
        Long getOwnerMemberId();
        Long getChildId();
        Boolean getFather();
        Boolean getMother();
    }
}
