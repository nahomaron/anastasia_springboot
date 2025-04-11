package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMapper tenantMapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    @Override
    public TenantEntity convertTenantToEntity(TenantDTO tenantDTO) {
        return tenantMapper.tenantDTOToEntity(tenantDTO);
    }

    @Override
    public TenantDTO convertTenantToDTO(TenantEntity tenantEntity) {
        return tenantMapper.tenantEntityToDTO(tenantEntity);
    }

    @Transactional
    @Override
    public void subscribeTenant(TenantDTO tenantDTO) throws MessagingException {

        TenantEntity tenantEntity = TenantEntity.builder()
                .tenantType(tenantDTO.getTenantType())
                .ownerName(tenantDTO.getOwnerName())
                .phoneNumber(tenantDTO.getPhoneNumber())
                .subscriptionPlan(tenantDTO.getSubscriptionPlan())
                .build();

        TenantEntity savedTenant = tenantRepository.save(tenantEntity);

        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseThrow(() -> new RuntimeException("Owner role not found"));


        UserEntity adminUser = UserEntity.builder()
                .fullName(tenantDTO.getOwnerName())
                .email(tenantDTO.getEmail())
                .password(tenantDTO.getPassword())
                .tenant(savedTenant)
                .roles(Set.of(ownerRole))
                .build();

        authService.createUser(adminUser);
    }

    @Override
    public Page<TenantEntity> findAll(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    public List<TenantEntity> getTenants(){
        return tenantRepository.findAll();
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
