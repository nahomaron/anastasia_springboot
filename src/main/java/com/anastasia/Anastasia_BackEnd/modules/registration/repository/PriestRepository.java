package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriestRepository extends JpaRepository<PriestEntity, Long> {
    boolean existsByPriestNumber(String priestNumber);

    Optional<PriestEntity> findByPhoneNumber(String phoneNumber);

    List<PriestEntity> findByChurch_ChurchId(Long churchId);

    long countByChurch_Tenant_Id(java.util.UUID tenantId);
}
