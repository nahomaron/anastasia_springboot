package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.model.role.Role;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import com.anastasia.Anastasia_BackEnd.model.user.UserType;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.RoleRepository;
import com.anastasia.Anastasia_BackEnd.repository.auth.UserRepository;
import com.anastasia.Anastasia_BackEnd.service.auth.AuthService;
import com.anastasia.Anastasia_BackEnd.service.sms.PhoneVerificationService;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
    private final PhoneVerificationService phoneVerificationService;   // NEW

    @Override
    public TenantEntity convertTenantToEntity(TenantDTO tenantDTO) {
        return tenantMapper.tenantDTOToEntity(tenantDTO);
    }

    @Override
    public TenantDTO convertTenantToDTO(TenantEntity tenantEntity) {
        return tenantMapper.tenantEntityToDTO(tenantEntity);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenants_page", allEntries = true),
            @CacheEvict(value = "tenants_all", allEntries = true),
            @CacheEvict(value = "tenants", allEntries = true),
            @CacheEvict(value = "tenants_by_phone", allEntries = true)
    })
    @Transactional
    @Override
    public void subscribeTenant(TenantDTO tenantDTO) throws MessagingException {

        // 1. ADD DUPLICATE PHONE NUMBER CHECK
        if (tenantRepository.existsByPhoneNumber(tenantDTO.getPhoneNumber())) {
            throw new DuplicateKeyException("Phone number already in use.");
            // Or throw a more specific exception that your controller can handle and return a 409 Conflict.
        }

        // 2. Add Email Duplication check (Good Practice)
        if (userRepository.existsByEmail(tenantDTO.getEmail())) {
            throw new DuplicateKeyException("Email address already in use.");
        }

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
                .roles(new HashSet<>(Set.of(ownerRole)))
                .userType(UserType.TENANT)
                .build();

        authService.createUser(adminUser);

        // Send OTP after account creation
        phoneVerificationService.startVerification(tenantDTO.getPhoneNumber());
    }

    @Caching(evict = {
            @CacheEvict(value = "tenants_by_phone", key = "#phone"),
            @CacheEvict(value = "tenants", allEntries = true),
            @CacheEvict(value = "tenants_page", allEntries = true),
            @CacheEvict(value = "tenants_all", allEntries = true)
    })
    @Transactional
    @Override
    public boolean verifyTenantPhone(String phone, String rawOtp) {
        if (!phoneVerificationService.confirmOtp(phone, rawOtp)) {
            return false;
        }

        tenantRepository.findByPhoneNumber(phone).ifPresent(tenant -> {
            tenant.setPhoneVerified(true);
            tenantRepository.save(tenant);  // Save verification update
//            checkAndActivateTenant(tenant); // Centralized logic
        });
        // update verified flag
        return true;
    }

//    public void checkAndActivateTenant(TenantEntity tenant) {
//        if (!tenant.isPhoneVerified()) return;
//
//        userRepository.findTenantAdmin(tenant.getId())
//                .filter(UserEntity::isVerified)  // email is verified
//                .ifPresent(user -> {
//                    tenant.setActiveTenant(true);
//                    tenantRepository.save(tenant);
//                });
//    }

    @Cacheable(value = "tenants_page", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<TenantEntity> findAll(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    @Cacheable(value = "tenants_all")
    public List<TenantEntity> getTenants(){
        return tenantRepository.findAll();
    }

    @Cacheable(value = "tenants", key = "#tenantId")
    @Override
    public Optional<TenantEntity> findTenantById(UUID tenantId) {
        return tenantRepository.findById(tenantId);
    }

    @Cacheable(value = "tenants_by_phone", key = "#phone")
    @Override
    public Optional<TenantEntity> findTenantByPhoneNumber(String phone) {
        return tenantRepository.findByPhoneNumber(phone);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenants", key = "#tenantId"),
            @CacheEvict(value = "tenants_page", allEntries = true),
            @CacheEvict(value = "tenants_all", allEntries = true),
            @CacheEvict(value = "tenants_by_phone", allEntries = true)
    })
    @Override
    public void unsubscribeTenant(UUID tenantId) {
        TenantEntity tenantToBeUnsubscribed = tenantRepository.findById(tenantId)
                .orElseThrow(SecurityException::new);

        tenantToBeUnsubscribed.setActiveTenant(false);
        tenantRepository.save(tenantToBeUnsubscribed);
    }

    @Caching(evict = {
            @CacheEvict(value = "tenants", key = "#tenantId"),
            @CacheEvict(value = "tenants_page", allEntries = true),
            @CacheEvict(value = "tenants_all", allEntries = true),
            @CacheEvict(value = "tenants_by_phone", allEntries = true)
    })
    @Override
    public void updateTenant(UUID tenantId, TenantDTO tenantDTO) {
        tenantRepository.findById(tenantId).ifPresent(tenantEntity -> {
           Optional.ofNullable(tenantDTO.getOwnerName()).ifPresent(tenantEntity::setOwnerName);
           Optional.ofNullable(tenantDTO.getTenantType()).ifPresent(tenantEntity::setTenantType);
           Optional.ofNullable(tenantDTO.getPhoneNumber()).ifPresent(tenantEntity::setPhoneNumber);

           tenantRepository.save(tenantEntity);
        }
        );

    }

}
