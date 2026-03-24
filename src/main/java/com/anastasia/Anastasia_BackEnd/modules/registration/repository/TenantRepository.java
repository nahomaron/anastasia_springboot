package com.anastasia.Anastasia_BackEnd.modules.registration.repository;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByPhoneNumber(String phone);

    Optional<TenantEntity> findFirstByDemoTemplateTrueAndDeletedAtIsNull();

    @Modifying
    @Query("update TenantEntity t set t.demoTemplate = false where t.demoTemplate = true")
    int clearDemoTemplateFlags();

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsBySlug(String slug);
}
