package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ChildRepository extends JpaRepository<Child_MemberEntity, Long>, JpaSpecificationExecutor<Child_MemberEntity> {
    boolean existsByMembershipNumber(String membershipNumber);

    List<Child_MemberEntity> findByFatherIdOrMotherId(Long fatherId, Long motherId);

    @Query("""
            SELECT c FROM Child_MemberEntity c
            WHERE c.tenantId = :tenantId
              AND (c.father.id = :memberId OR c.mother.id = :memberId)
            ORDER BY c.createdAt DESC, c.firstName ASC, c.fatherName ASC, c.grandFatherName ASC
            """)
    List<Child_MemberEntity> findFamilyChildren(@Param("tenantId") UUID tenantId, @Param("memberId") Long memberId);

    Optional<Child_MemberEntity> findByIdAndTenantId(Long id, UUID tenantId);

    List<Child_MemberEntity> findAllByIdInAndTenantId(Set<Long> ids, UUID tenantId);

    Page<Child_MemberEntity> findByStatusNotAndTenantId(String status, UUID tenantId, Pageable pageable);

    Page<Child_MemberEntity> findByStatusAndTenantId(String status, UUID tenantId, Pageable pageable);

    long countByStatusNotAndTenantId(String status, UUID tenantId);
    long countByTenantIdAndStatusIn(UUID tenantId, List<String> statuses);
    long countByTenantIdAndPriestNumberAndStatusNot(UUID tenantId, String priestNumber, String status);

    Page<Child_MemberEntity> findByTenantIdAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Child_MemberEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("""
            SELECT c FROM Child_MemberEntity c
            WHERE c.tenantId = :tenantId
              AND c.status <> :status
              AND (
                LOWER(COALESCE(c.firstName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.fatherName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.grandFatherName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.firstNameT, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.fatherNameT, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.grandFatherNameT, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.membershipNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Child_MemberEntity> searchNonPending(@Param("q") String query,
                                              @Param("status") String status,
                                              @Param("tenantId") UUID tenantId,
                                              Pageable pageable);
}
