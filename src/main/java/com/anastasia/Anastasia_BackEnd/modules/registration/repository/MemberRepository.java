package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberLifecycleStatus;
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
import java.util.Set;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<Adult_MemberEntity, Long>, JpaSpecificationExecutor<Adult_MemberEntity> {

    boolean existsByMembershipNumber(String membershipNumber);

    List<Adult_MemberEntity> findByBirthday(LocalDate date);

    Optional<Adult_MemberEntity> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<Adult_MemberEntity> findByIdAndTenantId(Long id, UUID tenantId);

    Optional<Adult_MemberEntity> findByMembershipNumberAndTenantId(String membershipNumber, UUID tenantId);

    List<Adult_MemberEntity> findAllByIdInAndTenantId(Set<Long> ids, UUID tenantId);

    Optional<Adult_MemberEntity> findFirstBySpouseIdNumberAndTenantId(String spouseIdNumber, UUID tenantId);

    Page<Adult_MemberEntity> findByStatusValueNotAndTenantId(MemberLifecycleStatus status, UUID tenantId, Pageable pageable);

    Page<Adult_MemberEntity> findByStatusValueAndTenantId(MemberLifecycleStatus status, UUID tenantId, Pageable pageable);

    long countByStatusValueNotAndTenantId(MemberLifecycleStatus status, UUID tenantId);
    long countByTenantIdAndStatusValueIn(UUID tenantId, List<String> statuses);
    long countByTenantIdAndPriestNumberAndStatusValueNot(UUID tenantId, String priestNumber, MemberLifecycleStatus status);

    Page<Adult_MemberEntity> findByTenantIdAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Adult_MemberEntity> findByTenantIdAndPriestNumberAndStatusValue(UUID tenantId, String priestNumber, MemberLifecycleStatus status, Pageable pageable);

    Page<Adult_MemberEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<Adult_MemberEntity> findByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);

    @Query("""
            SELECT m FROM Adult_MemberEntity m
            WHERE m.tenantId = :tenantId
              AND m.statusValue <> :status
              AND (
                LOWER(COALESCE(m.firstName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.fatherName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.grandFatherName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.firstNameLocal, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.fatherNameLocal, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.grandFatherNameLocal, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.membershipNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(m.profession, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Adult_MemberEntity> searchNonPending(@Param("q") String query,
                                              @Param("status") MemberLifecycleStatus status,
                                              @Param("tenantId") UUID tenantId,
                                              Pageable pageable);
}
