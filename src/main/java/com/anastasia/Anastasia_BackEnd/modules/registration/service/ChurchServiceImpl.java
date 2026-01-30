package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.modules.registration.repository.TenantRepository;
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

    @Override
    public ChurchEntity convertToEntity(ChurchDTO churchDTO) {
        ChurchEntity entity = churchMapper.churchDTOToEntity(churchDTO);

        if (entity.getTenant() == null && TenantContext.getTenantId() != null) {
            UUID tenantId = TenantContext.getTenantId();
            TenantEntity tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new IllegalStateException("Tenant not found for context ID"));
            entity.setTenant(tenant);
        }

        if (entity.getProfilePicture() != null && entity.getTenant() != null) {
            entity.getProfilePicture().setAvatarType(AvatarType.CHURCH);
            entity.getProfilePicture().setOwnerId(entity.getTenant().getId());
        }

        return entity;
    }

    @Override
    public ChurchDTO convertToDTO(ChurchEntity churchEntity) {
        return churchMapper.churchEntityToDTO(churchEntity);
    }

    @Override
    public ChurchResponse convertToResponse(ChurchEntity churchEntity) {
        return churchMapper.churchEntityToResponse(churchEntity);
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
    public String createChurch(ChurchEntity churchEntity) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set in the context");
        }

        TenantEntity tenant = tenantRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new InvalidDataAccessApiUsageException("No valid tenant found"));

        churchEntity.setTenant(tenant);


        churchEntity.setChurchNumber(generateUniqueChurchNumber(churchEntity.getChurchName(), 5));
        var savedChurch = churchRepository.save(churchEntity);

        // assign the church back to the tenant
        tenant.assignChurch(savedChurch);
        tenantRepository.save(tenant);

        return savedChurch.getChurchNumber();
    }

//    @Cacheable(value = "churches_all", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Page<ChurchResponse> findAll(Pageable pageable, String query) {
        Page<ChurchEntity> churches;
        if (query == null || query.isBlank()) {
            churches = churchRepository.findAll(pageable);
        } else {
            churches = churchRepository.search(query.trim(), pageable);
        }
        return churches.map(churchMapper::churchEntityToResponse);
    }

//    @Cacheable(value = "churches", keyGenerator = "tenantAwareKeyGenerator")
    @Override
    public Optional<ChurchEntity> findOne(Long churchId) {
        return churchRepository.findById(churchId);
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
    public void updateChurch(Long churchId, ChurchEntity churchEntity) {
        ChurchEntity existingChurch = churchRepository.findById(churchId)
                .orElseThrow(()-> new EntityNotFoundException("Church Not Found"));

        churchEntity.setChurchId(existingChurch.getChurchId());
        churchEntity.setTenant(existingChurch.getTenant());
        churchEntity.setChurchNumber(existingChurch.getChurchNumber()); // ✅ Fix here
        churchRepository.save(churchEntity);

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

        String baseLetter = null;

        // Ensure a valid church name is provided
        if (churchName != null && !churchName.isBlank()) {
            if (churchName.startsWith("st.")) {
                baseLetter = churchName.substring(3, 5).toUpperCase();
            } else {
                baseLetter = churchName.substring(0, 2).toUpperCase();
            }
        }

        String churchNumber;

        do {
            churchNumber = securityUtils.generateUniqueIDNumber(length, baseLetter);
        } while (churchRepository.existsByChurchNumber(churchNumber)); // Keep generating if it already exists
        return churchNumber;
    }
}
