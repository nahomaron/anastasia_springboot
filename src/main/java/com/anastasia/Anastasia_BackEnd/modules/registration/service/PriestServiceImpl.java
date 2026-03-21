package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.PriestMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestStatus;
import com.anastasia.Anastasia_BackEnd.core.auth.role.Role;
import com.anastasia.Anastasia_BackEnd.core.auth.role.RoleType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserStatus;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserType;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.PriestRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.RoleRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.repository.UserRepository;
import com.anastasia.Anastasia_BackEnd.core.auth.service.AuthServiceImpl;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriestServiceImpl implements PriestService{

    private final PriestMapper priestMapper;
    private final PriestRepository priestRepository;
    private final ChurchRepository churchRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuthServiceImpl authService;
    private final RoleRepository roleRepository;
    private final SecurityUtils securityUtils;
    private final LocalizedMessageService messageService;

    @Override
    public PriestEntity convertToEntity(PriestDTO priestDTO) {
        return priestMapper.priestDTOToEntity(priestDTO);
    }

    @Override
    public PriestDTO convertToDTO(PriestEntity priestEntity) {
        return priestMapper.priestEntityToDTO(priestEntity);
    }

    @Override
    public PriestResponse convertToResponse(PriestEntity priestEntity) {
        return priestMapper.priestEntityToResponse(priestEntity);
    }



//    @Caching(
//            evict = {@CacheEvict(value = "priests_all",
//                    keyGenerator = "tenantAwareKeyGenerator",
//                    allEntries = true),
//                    @CacheEvict(value = "priests_by_church",
//                    keyGenerator = "tenantAwareKeyGenerator",
//                    allEntries = true)
//            }
//    )
    @Override
    @Transactional
    public void registerPriest(PriestDTO priestDTO) {

        // Try to find an existing user by email
        UserEntity priestUser = userRepository.findByEmail(priestDTO.getPersonalEmail()).orElse(null);

        String sanitizedChurchNumber = sanitizeChurchNumber(priestDTO.getChurchNumber());

        String priestNumber = generateUniquePriestNumber(6);

        Role priestRole = roleRepository.findByRoleName(RoleType.PRIEST.name())
                .orElseThrow(() -> new RuntimeException(messageService.get(
                        "role.priest.notFound",
                        "Priest role not found"
                )));

        if (priestUser == null) {
            // If user does not exist, create a new one
            priestUser = UserEntity.builder()
                    .fullName(priestDTO.getFirstName() + " " + priestDTO.getFatherName() + " " + priestDTO.getGrandFatherName())
                    .email(priestDTO.getPersonalEmail())
                    .password(passwordEncoder.encode(priestDTO.getPassword()))
                    .roles(new HashSet<>(Set.of(priestRole)))
                    .status(UserStatus.PENDING_VERIFICATION)
                    .userType(UserType.PRIEST)
                    .priestNumber(priestNumber)
                    .build();

            ensureBackendManagedUserStatus(priestUser);

            // Save the newly created priest user
            try {
                var savedPriest = userRepository.save(priestUser);
                authService.sendValidationEmail(savedPriest);
            } catch (Exception e) {
                throw new RuntimeException(messageService.get(
                        "auth.user.creationFailed",
                        "User creation failed: {0}",
                        e.getMessage()
                ));
            }
        } else {
            priestUser.setPriestNumber(priestNumber);
            ensureBackendManagedUserStatus(priestUser);
            userRepository.save(priestUser);
        }


        if(sanitizedChurchNumber == null && priestDTO.getTenantId() == null){
            throw new IllegalStateException(messageService.get(
                    "registration.priest.churchOrTenant.required",
                    "A priest should provide church number or be a tenant"
            ));
        }
        // Start building the PriestEntity
        PriestEntity.PriestEntityBuilder priestBuilder = PriestEntity.builder()
                .user(priestUser)
                .priestNumber(priestNumber)
                .churchNumber(sanitizedChurchNumber)
                .avatar(enrichAvatar(priestMapper.map(priestDTO.getAvatar()), priestUser))
                .prefixes(priestDTO.getPrefixes())
                .firstName(priestDTO.getFirstName())
                .fatherName(priestDTO.getFatherName())
                .grandFatherName(priestDTO.getGrandFatherName())
                .phoneNumber(priestDTO.getPhoneNumber())
                .churchEmail(priestDTO.getChurchEmail())
                .priesthoodCardId(priestDTO.getPriesthoodCardId())
                .priesthoodCardScan(priestDTO.getPriesthoodCardScan())
                .birthdate(priestDTO.getBirthdate())
                .languages(normalizeLanguages(priestDTO.getLanguages()))
                .levelOfEducation(priestDTO.getLevelOfEducation())
                .address(priestDTO.getAddress())
                .status(PriestStatus.PENDING)
                .spiritualChildren(0)
                .isActive(false);

        boolean priestIsTenant = priestDTO.getTenantId() != null;
        boolean priestIsUnderChurch = sanitizedChurchNumber != null;

        // Validation: A priest cannot be both a tenant and belong to a church
        if (priestIsTenant && priestIsUnderChurch) {
            throw new IllegalStateException(messageService.get(
                    "registration.priest.churchAndTenant.conflict",
                    "A priest cannot be both a tenant and belong to a church. Choose one."
            ));
        }

        // If the priest is a tenant, associate with tenant
        if (priestIsTenant) {
            TenantEntity tenantFound = tenantRepository.findById(priestDTO.getTenantId())
                    .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                            "tenant.notFound.withId",
                            "Tenant with ID {0} does not exist",
                            priestDTO.getTenantId()
                    )));
            priestBuilder.tenant(tenantFound);
            priestUser.assignTenant(tenantFound);
            userRepository.save(priestUser);
        }
        // If the priest is under a church, associate with church
        else if (priestIsUnderChurch) {
            ChurchEntity churchFound = churchRepository.findByChurchNumber(sanitizedChurchNumber)
                    .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                            "church.notFound.withNumber",
                            "Church with number {0} not found",
                            sanitizedChurchNumber
                    )));
            priestBuilder.church(churchFound);
            TenantEntity churchTenant = churchFound.getTenant();
            if (churchTenant == null) {
                throw new IllegalStateException(messageService.get(
                        "registration.priest.churchTenant.missing",
                        "Church tenant is missing for church {0}",
                        sanitizedChurchNumber
                ));
            }
            priestUser.assignTenant(churchTenant);
            userRepository.save(priestUser);
        }

        // Save the priest entity
        priestRepository.save(priestBuilder.build());
    }

    private void ensureBackendManagedUserStatus(UserEntity user) {
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.PENDING_VERIFICATION);
        }
    }

//    @Cacheable(value = "priests_all", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<PriestResponse> findAllPriests(Pageable pageable) {
        return priestRepository.findAll(pageable)
                .map(priestMapper::priestEntityToResponse);
    }

//    @Cacheable(value = "priests", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Optional<PriestResponse> findPriestById(Long priestId) {
        return priestRepository.findById(priestId)
                .map(priestMapper::priestEntityToResponse);
    }

//    @Cacheable(value = "priests_by_church", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public List<PriestResponse> findPriestsByChurchId(Long churchId) {
        return priestRepository.findByChurch_ChurchId(churchId)
                .stream()
                .map(priestMapper::priestEntityToResponse)
                .toList();
    }

    @Override
    public List<PriestResponse> findActivePriestsByChurchId(Long churchId) {
        return priestRepository.findByChurch_ChurchIdAndStatus(churchId, PriestStatus.ACTIVE)
                .stream()
                .map(priestMapper::priestEntityToResponse)
                .toList();
    }

//    @Caching(
//            put = {@CachePut(value = "priests",
//                    keyGenerator = "tenantAwareKeyGenerator")},
//            evict = {@CacheEvict( value = "priests_all",
//                    keyGenerator = "tenantAwareKeyGenerator",  allEntries = true),
//                    @CacheEvict(value = "priests_by_church",
//                    keyGenerator = "tenantAwareKeyGenerator",
//                    allEntries = true)}
//    )
    @Override
    public PriestResponse updatePriestDetails(Long priestId, PriestEntity priestEntity, Boolean isActive) {

        return priestRepository.findById(priestId).map(foundPriest -> {
            Optional.ofNullable(priestEntity.getChurch()).ifPresent(foundPriest::setChurch);
            Optional.ofNullable(priestEntity.getAvatar())
                    .map(avatar -> enrichAvatar(avatar, foundPriest.getUser()))
                    .ifPresent(foundPriest::setAvatar);
            Optional.ofNullable(priestEntity.getStatus()).ifPresent(foundPriest::setStatus);

            Optional.ofNullable(priestEntity.getPrefixes()).ifPresent(foundPriest::setPrefixes);
            Optional.ofNullable(priestEntity.getFirstName()).ifPresent(foundPriest::setFirstName);
            Optional.ofNullable(priestEntity.getFatherName()).ifPresent(foundPriest::setFatherName);
            Optional.ofNullable(priestEntity.getGrandFatherName()).ifPresent(foundPriest::setGrandFatherName);
            Optional.ofNullable(priestEntity.getPhoneNumber()).ifPresent(foundPriest::setPhoneNumber);

            Optional.ofNullable(priestEntity.getChurchEmail()).ifPresent(foundPriest::setChurchEmail);

            Optional.ofNullable(priestEntity.getBirthdate()).ifPresent(foundPriest::setBirthdate);

            Optional.ofNullable(priestEntity.getAddress())
                    .ifPresent(address -> foundPriest.setAddress(mergeAddress(foundPriest.getAddress(), address)));
            Optional.ofNullable(priestEntity.getLanguages())
                    .map(this::normalizeLanguages)
                    .ifPresent(foundPriest::setLanguages);
            Optional.ofNullable(priestEntity.getLevelOfEducation()).ifPresent(foundPriest::setLevelOfEducation);
            Optional.ofNullable(priestEntity.getPriesthoodCardId()).ifPresent(foundPriest::setPriesthoodCardId);
            Optional.ofNullable(priestEntity.getPriesthoodCardScan()).ifPresent(foundPriest::setPriesthoodCardScan);
            Optional.ofNullable(isActive).ifPresent(foundPriest::setActive);

            PriestEntity saved = priestRepository.save(foundPriest);
            return priestMapper.priestEntityToResponse(saved);
        }).orElseThrow(() -> new UsernameNotFoundException("Priest not found"));
    }

    private Address mergeAddress(Address current, Address incoming) {
        Address merged = current == null ? new Address() : current;

        if (incoming.getAddressLine1() != null) {
            merged.setAddressLine1(incoming.getAddressLine1());
        }
        if (incoming.getAddressLine2() != null) {
            merged.setAddressLine2(incoming.getAddressLine2());
        }
        if (incoming.getCity() != null) {
            merged.setCity(incoming.getCity());
        }
        if (incoming.getStateProvince() != null) {
            merged.setStateProvince(incoming.getStateProvince());
        }
        if (incoming.getCountry() != null) {
            merged.setCountry(incoming.getCountry());
        }
        if (incoming.getPostalCode() != null) {
            merged.setPostalCode(incoming.getPostalCode());
        }

        return merged;
    }

    private ImageAssetEntity enrichAvatar(ImageAssetEntity avatar, UserEntity user) {
        if (avatar == null) {
            return null;
        }
        if (avatar.getOwnerId() == null && user != null) {
            avatar.setOwnerId(user.getUuid());
        }
        if (avatar.getImageAssetType() == null) {
            avatar.setImageAssetType(ImageAssetType.USER);
        }
        return avatar;
    }

    private Set<String> normalizeLanguages(Set<String> languages) {
        if (languages == null || languages.isEmpty()) {
            return new HashSet<>();
        }
        return languages.stream()
                .filter(language -> language != null && !language.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(HashSet::new));
    }

//    @Caching(
//            evict = {
//                    @CacheEvict(value = "priests",
//                            keyGenerator = "tenantAwareKeyGenerator"
//                    ),
//                    @CacheEvict(value = "priests_all",
//                            keyGenerator = "tenantAwareKeyGenerator",
//                            allEntries = true
//                    ),
//                    @CacheEvict(value = "priests_by_church",
//                            keyGenerator = "tenantAwareKeyGenerator",
//                            allEntries = true
//                    )
//            }
//    )
    @Override
    public void deletePriest(Long priestId) {
        priestRepository.deleteById(priestId);
    }

    private String generateUniquePriestNumber(int length) {
        String baseLetter = "K";

        String priestNumber;
        do {
            priestNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (priestRepository.existsByPriestNumber(priestNumber)); // Keep generating if it already exists
        return priestNumber;
    }

    private String sanitizeChurchNumber(String churchNumber) {
        if (churchNumber == null) {
            return null;
        }
        String sanitized = churchNumber.replace("\"", "").trim();
        return org.springframework.util.StringUtils.hasText(sanitized) ? sanitized : null;
    }

}
