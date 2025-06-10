package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
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

    void subscribeTenant(TenantDTO tenantDTO) throws MessagingException;

    Page<TenantEntity> findAll(Pageable pageable);

    Optional<TenantEntity> findTenantById(UUID tenantId);

    void unsubscribeTenant(UUID tenantId);

    void updateTenant(UUID tenantId, @Valid TenantDTO tenantDTO);
}
