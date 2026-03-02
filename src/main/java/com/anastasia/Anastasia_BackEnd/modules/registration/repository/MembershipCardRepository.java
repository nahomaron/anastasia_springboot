package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.card.MembershipCardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipCardRepository extends JpaRepository<MembershipCardEntity, Long> {
    Optional<MembershipCardEntity> findByTenantIdAndMemberId(UUID tenantId, Long memberId);
    Optional<MembershipCardEntity> findByTenantIdAndMembershipNumber(UUID tenantId, String membershipNumber);
    Optional<MembershipCardEntity> findByIdAndTenantId(Long id, UUID tenantId);
    List<MembershipCardEntity> findByStatusAndExpirationDateBefore(MembershipCardStatus status, LocalDate date);
}
