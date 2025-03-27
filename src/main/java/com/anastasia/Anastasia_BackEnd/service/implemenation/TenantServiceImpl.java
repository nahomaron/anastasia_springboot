package com.anastasia.Anastasia_BackEnd.service.implemenation;

import com.anastasia.Anastasia_BackEnd.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.interfaces.TenantService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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


    @Transactional
    public TenantEntity subscribeUserAsTenant(UUID userId, TenantEntity tenantEntity) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTenant() != null) {
            throw new RuntimeException("User is already a tenant");
        }
        tenantEntity.setUser(user);
        tenantEntity.setActiveTenant(true);

        // Link tenant to user
        user.becomeTenant(tenantEntity);

        return tenantRepository.save(tenantEntity);
    }


}
