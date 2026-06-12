package com.anastasia.Anastasia_BackEnd.modules.platform.admin.repository;

import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessSessionEntity;
import com.anastasia.Anastasia_BackEnd.modules.platform.admin.model.SupportAccessSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportAccessSessionRepository extends JpaRepository<SupportAccessSessionEntity, UUID> {

    Optional<SupportAccessSessionEntity> findByIdAndActor_Uuid(UUID sessionId, UUID actorUserId);

    List<SupportAccessSessionEntity> findTop20ByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    List<SupportAccessSessionEntity> findByTenant_IdAndStatusOrderByCreatedAtDesc(UUID tenantId, SupportAccessSessionStatus status);
}
