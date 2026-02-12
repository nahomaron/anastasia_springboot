package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Adult_MemberEntity, Long>, JpaSpecificationExecutor<Adult_MemberEntity> {

    boolean existsByMembershipNumber(String membershipNumber);

    List<Adult_MemberEntity> findByBirthday(LocalDate date);

    Optional<Adult_MemberEntity> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<Adult_MemberEntity> findByIdAndTenantId(Long id, UUID tenantId);

    Page<Adult_MemberEntity> findByStatusNotAndTenantId(String status, UUID tenantId, Pageable pageable);

    Page<Adult_MemberEntity> findByStatusAndTenantId(String status, UUID tenantId, Pageable pageable);

    long countByStatusNotAndTenantId(String status, UUID tenantId);

    Page<Adult_MemberEntity> findByTenantIdAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Adult_MemberEntity> findByTenantIdAndPriestNumberAndStatus(UUID tenantId, String priestNumber, String status, Pageable pageable);

    @Query("""
            SELECT m FROM Adult_MemberEntity m
            WHERE m.tenantId = :tenantId
              AND m.status <> :status
              AND (
                LOWER(COALESCE(m.firstName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.fatherName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.grandFatherName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.firstNameT, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.fatherNameT, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.grandFatherNameT, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.membershipNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.profession, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Adult_MemberEntity> searchNonPending(@Param("q") String query,
                                              @Param("status") String status,
                                              @Param("tenantId") UUID tenantId,
                                              Pageable pageable);
}
