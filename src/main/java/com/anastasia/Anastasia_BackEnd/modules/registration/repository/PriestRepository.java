package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PriestRepository extends JpaRepository<PriestEntity, Long> {
    boolean existsByPriestNumber(String priestNumber);

    Optional<PriestEntity> findByPhoneNumber(String phoneNumber);

    Optional<PriestEntity> findByPriestNumber(String priestNumber);
    Optional<PriestEntity> findByUser_Uuid(UUID userId);

    List<PriestEntity> findByChurch_ChurchId(Long churchId);
    List<PriestEntity> findByChurch_ChurchIdAndStatus(Long churchId, PriestStatus status);

    long countByChurch_Tenant_Id(java.util.UUID tenantId);
}
