package com.anastasia.Anastasia_BackEnd.modules.users.repository;

import com.anastasia.Anastasia_BackEnd.modules.users.model.TenantUserPermissionGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenantUserPermissionGrantRepository extends JpaRepository<TenantUserPermissionGrantEntity, Long> {

    List<TenantUserPermissionGrantEntity> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);

    void deleteByUserId(UUID userId);
}
