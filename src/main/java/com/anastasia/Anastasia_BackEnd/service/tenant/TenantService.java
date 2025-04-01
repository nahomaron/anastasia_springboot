package com.anastasia.Anastasia_BackEnd.service.tenant;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public interface TenantService {
//    TenantEntity subscribeUserAsTenant(UUID userId, TenantEntity tenantEntity);

    TenantEntity convertTenantToEntity(TenantDTO tenantDTO);

    TenantDTO convertTenantToDTO(TenantEntity tenantEntity);

    TenantEntity subscribeTenant(TenantEntity tenantEntity);

    Page<TenantEntity> findAll(Pageable pageable);

    Optional<TenantEntity> findTenantById(UUID tenantId);

    void unsubscribeTenant(UUID tenantId);
}
