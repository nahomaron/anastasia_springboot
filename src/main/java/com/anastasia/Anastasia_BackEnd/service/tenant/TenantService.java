package com.anastasia.Anastasia_BackEnd.service.tenant;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface TenantService {
    TenantEntity subscribeUserAsTenant(UUID userId, TenantEntity tenantEntity);

    TenantEntity convertTenantToEntity(TenantDTO tenantDTO);

    TenantDTO convertTenantToDTO(TenantEntity tenantEntity);
}
