package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.TenantMapper;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.BillingProvider;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.MembershipStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantAdminAssignmentEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantRole;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantSubscriptionEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.SubscriptionStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantAdminAssignmentRepository;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.PhoneVerificationService;
import com.anastasia.Anastasia_BackEnd.common.utils.PhoneNumberUtils;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import jakarta.mail.MessagingException;
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
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final ChurchRepository churchRepository;
    private final UserRepository userRepository;
    private final TenantMapper tenantMapper;
    private final ChurchMapper churchMapper;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PhoneVerificationService phoneVerificationService;   // NEW
    private final SecurityUtils securityUtils;
    private final TenantAdminAssignmentRepository tenantAdminAssignmentRepository;
    private final LocalizedMessageService messageService;

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
    public TenantEntity subscribeTenant(TenantDTO tenantDTO) throws MessagingException {
        String normalizedPhone = PhoneNumberUtils.normalize(tenantDTO.getPhoneNumber());
        tenantDTO.setPhoneNumber(normalizedPhone);
        tenantDTO.setOwnerEmail(normalizeOwnerEmail(tenantDTO.getOwnerEmail()));

        TenantEntity existingTenantForRetry = findExistingTenantForRetry(tenantDTO);
        if (existingTenantForRetry != null) {
            return existingTenantForRetry;
        }

        // 1. ADD DUPLICATE PHONE NUMBER CHECK
        if (tenantRepository.existsByPhoneNumber(tenantDTO.getPhoneNumber())) {
            throw new DuplicateKeyException(messageService.get(
                    "tenant.phone.duplicate",
                    "Phone number already in use."
            ));
            // Or throw a more specific exception that your controller can handle and return a 409 Conflict.
        }

        // 2. Add Email Duplication check (Good Practice)
        if (userRepository.existsByEmail(tenantDTO.getOwnerEmail())) {
            throw new DuplicateKeyException(messageService.get(
                    "tenant.email.duplicate",
                    "Email address already in use."
            ));
        }

        String resolvedDisplayName = resolveDisplayName(tenantDTO);
        String resolvedSlug = resolveUniqueSlug(tenantDTO.getSlug(), resolvedDisplayName);

        TenantEntity tenantEntity = TenantEntity.builder()
                .displayName(resolvedDisplayName)
                .slug(resolvedSlug)
                .tenantType(tenantDTO.getTenantType())
                .status(TenantStatus.DRAFT)
                .ownerName(tenantDTO.getOwnerName())
                .ownerEmail(tenantDTO.getOwnerEmail())
                .phoneNumber(tenantDTO.getPhoneNumber())
                .phoneVerified(Boolean.TRUE.equals(tenantDTO.getPhoneVerified()))
                .billingEmail(firstNonBlank(tenantDTO.getBillingEmail(), tenantDTO.getOwnerEmail()))
                .defaultTimezone(firstNonBlank(tenantDTO.getDefaultTimezone(), "UTC"))
                .defaultLocale(firstNonBlank(tenantDTO.getDefaultLocale(), "en"))
                .build();

        TenantSubscriptionEntity subscription = TenantSubscriptionEntity.builder()
                .plan(tenantDTO.getSubscriptionPlan())
                .status(SubscriptionStatus.TRIALING)
                .provider(BillingProvider.MANUAL)
                .build();
        tenantEntity.assignSubscription(subscription);

        TenantEntity savedTenant = tenantRepository.save(tenantEntity);

        if (tenantDTO.getTenantType() == TenantType.CHURCH) {
            if (tenantDTO.getChurch() == null) {
                throw new IllegalArgumentException(messageService.get(
                        "tenant.church.details.required",
                        "Church details are required for church tenants."
                ));
            }

            ChurchEntity churchEntity = churchMapper.churchDTOToEntity(tenantDTO.getChurch());
            churchEntity.setTenant(savedTenant);
            churchEntity.setUsesOurServices(true);

            if (churchEntity.getProfilePicture() != null) {
                churchEntity.getProfilePicture().setImageAssetType(ImageAssetType.CHURCH);
                churchEntity.getProfilePicture().setOwnerId(savedTenant.getId());
            }

            churchEntity.setChurchNumber(generateUniqueChurchNumber(churchEntity.getChurchName(), 5));
            ChurchEntity savedChurch = churchRepository.save(churchEntity);
            savedTenant.assignChurch(savedChurch);
            savedTenant = tenantRepository.save(savedTenant);
        }

        Role ownerRole = roleRepository.findByRoleName("OWNER")
                .orElseThrow(() -> new RuntimeException(messageService.get(
                        "role.owner.notFound",
                        "Owner role not found"
                )));

        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseThrow(() -> new RuntimeException(messageService.get(
                        "role.admin.notFound",
                        "Admin role not found"
                )));


        UserEntity adminUser = UserEntity.builder()
                .fullName(tenantDTO.getOwnerName())
                .email(tenantDTO.getOwnerEmail())
                .password(tenantDTO.getPassword())
                .tenant(savedTenant)
                .roles(new HashSet<>(Set.of(ownerRole, adminRole)))
                .userType(UserType.TENANT)
                .build();

        authService.createUser(adminUser);

        UUID primaryAdminUserId = Optional.ofNullable(adminUser.getUuid())
                .orElseGet(() -> userRepository.findByEmail(adminUser.getEmail()).map(UserEntity::getUuid).orElse(null));
        if (primaryAdminUserId != null) {
            TenantAdminAssignmentEntity primaryAdminAssignment = TenantAdminAssignmentEntity.builder()
                    .tenant(savedTenant)
                    .userId(primaryAdminUserId)
                    .role(TenantRole.PRIMARY_ADMIN)
                    .status(MembershipStatus.ACTIVE)
                    .isBillingContact(true)
                    .createdByUserId(primaryAdminUserId)
                    .updatedByUserId(primaryAdminUserId)
                    .build();
            tenantAdminAssignmentRepository.save(primaryAdminAssignment);
        }

        // Send OTP after account creation
        phoneVerificationService.startVerification(tenantDTO.getPhoneNumber());
        return savedTenant;
    }

    /**
     * Treat duplicate submissions with the same email + phone + tenant as idempotent retries.
     * This lets the client safely retry onboarding requests after network timeouts.
     */
    private TenantEntity findExistingTenantForRetry(TenantDTO tenantDTO) {
        if (!StringUtils.hasText(tenantDTO.getOwnerEmail()) || !StringUtils.hasText(tenantDTO.getPhoneNumber())) {
            return null;
        }

        Optional<UserEntity> existingUser = userRepository.findByEmail(tenantDTO.getOwnerEmail());
        Optional<TenantEntity> existingTenantByPhone = tenantRepository.findByPhoneNumber(tenantDTO.getPhoneNumber());

        if (existingUser.isEmpty() || existingTenantByPhone.isEmpty()) {
            return null;
        }

        UserEntity user = existingUser.get();
        TenantEntity tenant = existingTenantByPhone.get();
        if (user.getTenant() == null || user.getTenant().getId() == null) {
            return null;
        }

        boolean sameTenant = user.getTenant().getId().equals(tenant.getId());
        boolean sameTenantType = tenantDTO.getTenantType() == null || tenantDTO.getTenantType().equals(tenant.getTenantType());
        boolean sameOwner = !StringUtils.hasText(tenantDTO.getOwnerName()) || tenantDTO.getOwnerName().equalsIgnoreCase(tenant.getOwnerName());

        return (sameTenant && sameTenantType && sameOwner) ? tenant : null;
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
        String normalizedPhone = PhoneNumberUtils.normalize(phone);
        if (!phoneVerificationService.confirmOtp(normalizedPhone, rawOtp)) {
            return false;
        }

        tenantRepository.findByPhoneNumber(normalizedPhone)
                .or(() -> tenantRepository.findByPhoneNumber(phone))
                .ifPresent(tenant -> {
            tenant.setPhoneVerified(true);
            tenant.setPhoneVerifiedAt(Instant.now());
            if (tenant.getStatus() == TenantStatus.DRAFT) {
                tenant.setStatus(TenantStatus.ACTIVE);
                if (tenant.getActivatedAt() == null) {
                    tenant.setActivatedAt(Instant.now());
                }
            }
            tenantRepository.save(tenant);
        });
        return true;
    }

    @Cacheable(value = "tenants_page", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<TenantDTO> findAll(Pageable pageable) {
        return tenantRepository.findAll(pageable).map(this::convertTenantToDTO);
    }

    public List<TenantEntity> getTenants(){
        return tenantRepository.findAll();
    }

    @Override
    public Optional<TenantEntity> findTenantEntityById(UUID tenantId) {
        return tenantRepository.findById(tenantId);
    }

    @Cacheable(value = "tenants", key = "#tenantId")
    @Override
    public Optional<TenantDTO> findTenantDtoById(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(this::convertTenantToDTO);
    }

    @Override
    public Optional<TenantEntity> findTenantEntityByPhoneNumber(String phone) {
        return tenantRepository.findByPhoneNumber(PhoneNumberUtils.normalize(phone));
    }

    @Cacheable(value = "tenants_by_phone", key = "#phone")
    @Override
    public Optional<TenantDTO> findTenantDtoByPhoneNumber(String phone) {
        return tenantRepository.findByPhoneNumber(PhoneNumberUtils.normalize(phone)).map(this::convertTenantToDTO);
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

        if (tenantToBeUnsubscribed.getStatus() == TenantStatus.ACTIVE) {
            tenantToBeUnsubscribed.setStatus(TenantStatus.DEACTIVATED);
        }
        if (tenantToBeUnsubscribed.getDeactivatedAt() == null) {
            tenantToBeUnsubscribed.setDeactivatedAt(Instant.now());
        }
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
           Optional.ofNullable(tenantDTO.getDisplayName()).filter(StringUtils::hasText).map(String::trim).ifPresent(tenantEntity::setDisplayName);
           Optional.ofNullable(tenantDTO.getSlug())
                   .filter(StringUtils::hasText)
                   .map(slug -> resolveUniqueSlug(slug, tenantEntity.getDisplayName()))
                   .ifPresent(tenantEntity::setSlug);
           Optional.ofNullable(tenantDTO.getTenantType()).ifPresent(tenantEntity::setTenantType);
           Optional.ofNullable(tenantDTO.getOwnerEmail())
                   .filter(StringUtils::hasText)
                   .map(this::normalizeOwnerEmail)
                   .ifPresent(tenantEntity::setOwnerEmail);
           Optional.ofNullable(tenantDTO.getPhoneNumber())
                   .map(PhoneNumberUtils::normalize)
                   .ifPresent(tenantEntity::setPhoneNumber);
           Optional.ofNullable(tenantDTO.getDefaultTimezone()).filter(StringUtils::hasText).ifPresent(tenantEntity::setDefaultTimezone);
           Optional.ofNullable(tenantDTO.getDefaultLocale()).filter(StringUtils::hasText).ifPresent(tenantEntity::setDefaultLocale);
           Optional.ofNullable(tenantDTO.getBillingEmail()).filter(StringUtils::hasText).map(this::normalizeOwnerEmail).ifPresent(tenantEntity::setBillingEmail);
           Optional.ofNullable(tenantDTO.getStatus()).ifPresent(tenantEntity::setStatus);

           tenantRepository.save(tenantEntity);
        }
        );

    }

    private String generateUniqueChurchNumber(String churchName, int length) {
        String baseLetter = "CH";

        if (churchName != null) {
            String trimmed = churchName.trim();
            if (trimmed.length() >= 2) {
                if (trimmed.startsWith("st.") && trimmed.length() >= 5) {
                    baseLetter = trimmed.substring(3, 5).toUpperCase();
                } else {
                    baseLetter = trimmed.substring(0, 2).toUpperCase();
                }
            }
        }

        String churchNumber;
        do {
            churchNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (churchRepository.existsByChurchNumber(churchNumber));

        return churchNumber;
    }

    private String resolveDisplayName(TenantDTO tenantDTO) {
        if (StringUtils.hasText(tenantDTO.getDisplayName())) {
            return tenantDTO.getDisplayName().trim();
        }
        if (tenantDTO.getTenantType() == TenantType.CHURCH && tenantDTO.getChurch() != null
                && StringUtils.hasText(tenantDTO.getChurch().getChurchName())) {
            return tenantDTO.getChurch().getChurchName().trim();
        }
        return tenantDTO.getOwnerName().trim();
    }

    private String resolveUniqueSlug(String requestedSlug, String displayName) {
        String base = StringUtils.hasText(requestedSlug) ? requestedSlug : displayName;
        String normalized = slugify(base);
        String candidate = normalized;
        int suffix = 2;
        while (tenantRepository.existsBySlug(candidate)) {
            candidate = normalized + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (!StringUtils.hasText(normalized)) {
            return "tenant-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return normalized;
    }

    private String normalizeOwnerEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        return fallback;
    }

}
