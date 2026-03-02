package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferRequestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MemberTransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemberTransferRequestRepository extends JpaRepository<MemberTransferRequestEntity, UUID> {

    Optional<MemberTransferRequestEntity> findByIdAndFromTenant_Id(UUID id, UUID fromTenantId);

    boolean existsByUserIdAndStatus(UUID userId, MemberTransferStatus status);
}
