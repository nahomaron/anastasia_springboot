package com.anastasia.Anastasia_BackEnd.service.interfaces;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;

import java.util.UUID;

public interface TenantService {
    TenantEntity subscribeUserAsTenant(UUID userId, TenantEntity tenantEntity);

    TenantEntity convertTenantToEntity(TenantDTO tenantDTO);

    TenantDTO convertTenantToDTO(TenantEntity tenantEntity);
}
