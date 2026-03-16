package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.common.i18n.LocalizedMessageService;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.common.utils.ChurchNumberUtils;
import com.anastasia.Anastasia_BackEnd.common.utils.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChurchServiceImpl implements ChurchService{

    private final ChurchRepository churchRepository;
    private final ChurchMapper churchMapper;
    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;
    private final LocalizedMessageService messageService;

    @Override
    public ChurchEntity convertToEntity(ChurchDTO churchDTO) {
        ChurchEntity entity = churchMapper.churchDTOToEntity(churchDTO);

        if (entity.getTenant() == null && TenantContext.getTenantId() != null) {
            UUID tenantId = TenantContext.getTenantId();
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalStateException(messageService.get(
                            "tenant.notFound.forContext",
                            "Tenant not found for context ID"
                    )));
            entity.setTenant(tenant);
        }

        if (entity.getProfilePicture() != null && entity.getTenant() != null) {
            stampProfilePicture(entity, entity.getProfilePicture());
        }

        return entity;
    }

    @Override
    public ChurchDTO convertToDTO(ChurchEntity churchEntity) {
        return churchMapper.churchEntityToDTO(churchEntity);
    }

    @Override
    public ChurchResponse convertToResponse(ChurchEntity churchEntity) {
        ChurchResponse response = churchMapper.churchEntityToResponse(churchEntity);
        response.setChurchProfileComplete(churchEntity.isComplete());
        return response;
    }

//    @Caching(
//            put = {@CachePut(value = "churches",
//                    key = "#churchId")
//            },
//            evict = {
//                    @CacheEvict(value = "churches_all",
//                        keyGenerator = "tenantAwareKeyGenerator",
//                        allEntries = true),
//                    @CacheEvict(value = "churches_all_list",
//                            keyGenerator = "tenantAwareKeyGenerator",
//                            allEntries = true)
//            }
//    )
    @Override
    public ChurchResponse createChurch(ChurchEntity churchEntity) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException(messageService.get(
                    "tenant.context.missing",
                    "Tenant ID is not set in the context"
            ));
        }

        TenantEntity tenant = tenantRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new InvalidDataAccessApiUsageException(messageService.get(
                        "tenant.invalid",
                        "No valid tenant found"
                )));

        churchEntity.setTenant(tenant);
        applyStatusLifecycle(churchEntity, null);

        churchEntity.setChurchNumber(generateUniqueChurchNumber(churchEntity.getChurchNameLocal(), 5));
        if (churchEntity.getProfilePicture() != null) {
            stampProfilePicture(churchEntity, churchEntity.getProfilePicture());
        }
        var savedChurch = churchRepository.save(churchEntity);

        // assign the church back to the tenant
        tenant.assignChurch(savedChurch);
        tenantRepository.save(tenant);

        return convertToResponse(savedChurch);
    }

//    @Cacheable(value = "churches_all", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<ChurchResponse> findAll(Pageable pageable, String query, Boolean usesOurServices) {
        String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();
        if (normalizedQuery == null) {
            return churchRepository.findAllFiltered(usesOurServices, pageable).map(this::convertToResponse);
        }
        return churchRepository.search(normalizedQuery, usesOurServices, pageable).map(this::convertToResponse);
    }

//    @Cacheable(value = "churches", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Optional<ChurchEntity> findOne(Long churchId) {
        return churchRepository.findById(churchId);
    }

    @Override
    public Optional<ChurchEntity> findOneByChurchNumber(String churchNumber) {
        return churchRepository.findByChurchNumber(churchNumber);
    }

    @Override
    public Optional<ChurchEntity> findOneByChurchNumberUsingOurServices(String churchNumber) {
        return churchRepository.findByChurchNumberAndUsesOurServicesTrue(churchNumber);
    }

//    @Cacheable(value = "churches_all_list", keyGenerator = "tenantAwareKeyGenerator")
    public List<ChurchEntity> getChurches(){
        return churchRepository.findAll();
    }


    @Override
    public boolean exists(Long churchId) {
        return churchRepository.existsById(churchId);
    }

//    @Caching(
//            evict = {
//                    @CacheEvict( value = "churches_all",
//                            keyGenerator = "tenantAwareKeyGenerator",
//                            allEntries = true),
//                    @CacheEvict(value = "churches_all_list",
//                            keyGenerator = "tenantAwareKeyGenerator",
//                            allEntries = true),
//                    @CacheEvict(value = "churches",
//                            key = "#churchId")
//            }
//    )
    @Override
    public ChurchResponse updateChurch(Long churchId, ChurchEntity churchEntity) {
        ChurchEntity existingChurch = churchRepository.findById(churchId)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get(
                        "church.notFound",
                        "Church Not Found"
                )));

        mergeChurch(existingChurch, churchEntity);
        applyStatusLifecycle(existingChurch, churchEntity.getStatus());
        ChurchEntity savedChurch = churchRepository.save(existingChurch);
        return convertToResponse(savedChurch);

    }

//    @Caching(
//            evict = {
//                    @CacheEvict(value = "churches",
//                            key = "#churchId"
//                    ),
//                    @CacheEvict(value = "churches_all",
//                            keyGenerator = "tenantAwareKeyGenerator",
//                            allEntries = true
//                    ),
//                    @CacheEvict(value = "churches_all_list",
//                            keyGenerator = "tenantAwareKeyGenerator",
//                            allEntries = true
//                    )
//            }
//    )
    @Override
    public void deleteChurch(Long churchId) {
        // todo -> deletion should be executed after 30 days of request
        churchRepository.deleteById(churchId);
    }

    private String generateUniqueChurchNumber(String churchName, int length) {
        String baseLetter = ChurchNumberUtils.derivePrefix(churchName);

        String churchNumber;

        do {
            churchNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (churchRepository.existsByChurchNumber(churchNumber)); // Keep generating if it already exists
        return churchNumber;
    }

    private void mergeChurch(ChurchEntity target, ChurchEntity incoming) {
        target.setPrefix(incoming.getPrefix());
        target.setPrefixLocal(incoming.getPrefixLocal());
        target.setChurchName(incoming.getChurchName());
        target.setChurchNameLocal(incoming.getChurchNameLocal());
        target.setNeighborhood(incoming.getNeighborhood());
        target.setNeighborhoodLocal(incoming.getNeighborhoodLocal());
        target.setDiocese(incoming.getDiocese());
        target.setAddress(incoming.getAddress());
        target.setEmail(incoming.getEmail());
        target.setPhone(incoming.getPhone());
        target.setTimezone(defaultTimezone(incoming.getTimezone(), target.getTimezone()));
        target.setLocale(defaultLocale(incoming.getLocale(), target.getLocale()));
        target.setDenomination(incoming.getDenomination());
        target.setDescription(incoming.getDescription());
        target.setUsesOurServices(incoming.isUsesOurServices());
        target.setGpsLocation(incoming.getGpsLocation());
        target.setLatitude(incoming.getLatitude());
        target.setLongitude(incoming.getLongitude());
        target.setWebsite(incoming.getWebsite());
        target.setInstagram(incoming.getInstagram());
        target.setYoutube(incoming.getYoutube());
        target.setFacebook(incoming.getFacebook());
        target.setStatus(incoming.getStatus() != null ? incoming.getStatus() : target.getStatus());
        target.setProfilePicture(incoming.getProfilePicture());

        if (target.getProfilePicture() != null) {
            stampProfilePicture(target, target.getProfilePicture());
        }
    }

    private void applyStatusLifecycle(ChurchEntity church, com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus requestedStatus) {
        var effectiveStatus = requestedStatus != null ? requestedStatus : church.getStatus();
        if (effectiveStatus == null) {
            effectiveStatus = com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus.DRAFT;
        }

        church.setStatus(effectiveStatus);

        if (effectiveStatus == com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus.ACTIVE) {
            if (church.getActivatedAt() == null) {
                church.setActivatedAt(Instant.now());
            }
            church.setDeactivatedAt(null);
            return;
        }

        if (effectiveStatus == com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus.INACTIVE
                || effectiveStatus == com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchStatus.ARCHIVED) {
            if (church.getDeactivatedAt() == null) {
                church.setDeactivatedAt(Instant.now());
            }
        }
    }

    private void stampProfilePicture(ChurchEntity church, com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity profilePicture) {
        profilePicture.setImageAssetType(ImageAssetType.CHURCH);
        profilePicture.setOwnerId(church.getTenant().getId());
        profilePicture.setTenantId(church.getTenant().getId());
    }

    private String defaultTimezone(String candidate, String fallback) {
        if (StringUtils.hasText(candidate)) {
            return candidate.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return "UTC";
    }

    private String defaultLocale(String candidate, String fallback) {
        if (StringUtils.hasText(candidate)) {
            return candidate.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return "en-US";
    }
}
