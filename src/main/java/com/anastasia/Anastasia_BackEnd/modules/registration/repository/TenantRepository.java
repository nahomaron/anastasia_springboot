package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByPhoneNumber(String phone);

    Optional<TenantEntity> findFirstByDemoTemplateTrueAndDeletedAtIsNull();

    @Modifying
    @Query("update TenantEntity t set t.demoTemplate = false where t.demoTemplate = true")
    int clearDemoTemplateFlags();

    @Query("""
        select t
        from TenantEntity t
        left join fetch t.subscription s
        where t.id = :tenantId
    """)
    Optional<TenantEntity> findWithSubscriptionById(@Param("tenantId") UUID tenantId);

    @Query("""
        select t
        from TenantEntity t
        left join fetch t.subscription s
        where t.deletedAt is null
          and t.demoWorkspace = true
    """)
    List<TenantEntity> findActiveDemoWorkspaces();

    @Query("""
        select t
        from TenantEntity t
        left join fetch t.subscription s
        where t.deletedAt is null
          and t.demoWorkspace = true
          and t.scheduledPurgeAt is not null
          and t.scheduledPurgeAt <= :now
    """)
    List<TenantEntity> findDemoWorkspacesDueForPurge(@Param("now") Instant now);

    @Query("""
        select t
        from TenantEntity t
        left join fetch t.subscription s
        where t.deletedAt is null
          and t.archiveScheduledAt is not null
          and t.archivedAt is null
          and t.archiveScheduledAt <= :now
    """)
    List<TenantEntity> findTenantsDueForArchive(@Param("now") Instant now);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsBySlug(String slug);
}
