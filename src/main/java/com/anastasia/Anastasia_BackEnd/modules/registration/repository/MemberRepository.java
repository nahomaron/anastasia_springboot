package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
