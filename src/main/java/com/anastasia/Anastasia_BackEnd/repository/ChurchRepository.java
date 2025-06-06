package com.anastasia.Anastasia_BackEnd.repository;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChurchRepository extends JpaRepository<ChurchEntity, Long> {

    Optional<ChurchEntity> findByChurchNumber(String churchNumber);

//    @Query("SELECT c FROM ChurchEntity c WHERE c.tenant.id = :tenantId")
//    Optional<ChurchEntity> findByTenantId(UUID tenantId);

    @Query("SELECT c FROM ChurchEntity c WHERE c.tenant.id = :tenantId")
    Optional<ChurchEntity> findByTenantId(@Param("tenantId") UUID tenantId);


    boolean existsByChurchNumber(String churchNumber);

}
