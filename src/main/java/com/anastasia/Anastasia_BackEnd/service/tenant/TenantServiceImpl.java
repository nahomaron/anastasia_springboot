package com.anastasia.Anastasia_BackEnd.service.tenant;

import com.anastasia.Anastasia_BackEnd.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMapper tenantMapper;

    @Override
    public TenantEntity convertTenantToEntity(TenantDTO tenantDTO) {
        return tenantMapper.tenantDTOToEntity(tenantDTO);
    }

    @Override
    public TenantDTO convertTenantToDTO(TenantEntity tenantEntity) {
        return tenantMapper.tenantEntityToDTO(tenantEntity);
    }

    @Override
    public TenantEntity subscribeTenant(TenantEntity tenantEntity) {
        tenantEntity.setActiveTenant(true);
         return tenantRepository.save(tenantEntity);
    }

    @Override
    public Page<TenantEntity> findAll(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Override
    public Optional<TenantEntity> findTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId);
    }

    @Override
    public void unsubscribeTenant(UUID tenantId) {
        TenantEntity tenantToBeUnsubscribed = tenantRepository.findById(tenantId)
                .orElseThrow(SecurityException::new);

        tenantToBeUnsubscribed.setActiveTenant(false);
        tenantRepository.save(tenantToBeUnsubscribed);
    }

}
