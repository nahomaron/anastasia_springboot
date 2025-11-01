package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long>, JpaSpecificationExecutor<MemberEntity> {

    boolean existsByMembershipNumber(String membershipNumber);

    List<MemberEntity> findByBirthday(LocalDate date);

    Optional<MemberEntity> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<MemberEntity> findByIdAndTenantId(Long id, UUID tenantId);
}
