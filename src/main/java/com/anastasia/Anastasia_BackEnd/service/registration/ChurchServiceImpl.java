package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import com.anastasia.Anastasia_BackEnd.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
        return churchMapper.churchDTOToEntity(churchDTO);
    }

    @Override
    public ChurchDTO convertToDTO(ChurchEntity churchEntity) {
        return churchMapper.churchEntityToDTO(churchEntity);
    }

    @Override
    public Page<ChurchEntity> findAll(Pageable pageable) {
        return churchRepository.findAll(pageable);
    }

    @Override
    public boolean exists(Long churchId) {
        return churchRepository.existsById(churchId);
    }

    @Override
    public void updateChurch(Long churchId, ChurchEntity churchEntity) {
        ChurchEntity church = churchRepository.findById(churchId)
                .orElseThrow(()-> new EntityNotFoundException("Church Not Found"));

        churchEntity.setChurchId(church.getChurchId());
        churchRepository.save(churchEntity);
    }

    @Override
    public void deleteChurch(Long churchId) {
        // todo -> deletion should be executed after 30 days of request
        churchRepository.deleteById(churchId);
    }

    @Override
    public Optional<ChurchEntity> findOne(Long churchId) {
        return churchRepository.findById(churchId);
    }

    public List<ChurchEntity> getChurches(){
        return churchRepository.findAll();
    }

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
