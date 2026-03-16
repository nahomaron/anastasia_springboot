package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("""
            SELECT c FROM ChurchEntity c
            WHERE (:usesOurServices IS NULL OR c.usesOurServices = :usesOurServices)
              AND (
                   LOWER(COALESCE(c.churchName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.churchNameLocal, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.churchNumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.prefix, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.prefixLocal, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.neighborhood, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.neighborhoodLocal, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.diocese, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.denomination, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.address.city, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.address.country, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<ChurchEntity> search(@Param("q") String query, @Param("usesOurServices") Boolean usesOurServices, Pageable pageable);

    @Query("""
            SELECT c FROM ChurchEntity c
            WHERE (:usesOurServices IS NULL OR c.usesOurServices = :usesOurServices)
            """)
    Page<ChurchEntity> findAllFiltered(@Param("usesOurServices") Boolean usesOurServices, Pageable pageable);

    Optional<ChurchEntity> findByChurchNumberAndUsesOurServicesTrue(String churchNumber);


    boolean existsByChurchNumber(String churchNumber);

}
