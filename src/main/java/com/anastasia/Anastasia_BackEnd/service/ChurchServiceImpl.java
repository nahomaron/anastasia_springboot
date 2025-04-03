package com.anastasia.Anastasia_BackEnd.service;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.mappers.ChurchMapper;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.model.tenant.TenantEntity;
import com.anastasia.Anastasia_BackEnd.repository.ChurchRepository;
import com.anastasia.Anastasia_BackEnd.repository.TenantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChurchServiceImpl implements ChurchService{

    private final ChurchRepository churchRepository;
    private final ChurchMapper churchMapper;
    private final TenantRepository tenantRepository;

    @Override
    public ChurchEntity convertToEntity(ChurchDTO churchDTO) {
        return churchMapper.churchDTOToEntity(churchDTO);
    }

    @Override
    public ChurchDTO convertToDTO(ChurchEntity churchEntity) {
        return churchMapper.churchEntityToDTO(churchEntity);
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

         return savedChurch.getChurchNumber();
    }

    private String generateChurchNumber(String churchName, int length) {

        String baseLetter = null;

        // Ensure a valid church name is provided
        if (churchName != null && !churchName.isBlank()) {
            if (churchName.startsWith("st.")) {
                baseLetter = churchName.substring(3, 4).toUpperCase();
            } else {
                baseLetter = churchName.substring(0, 1).toUpperCase();
            }
        }


        String characters = "01234456789";
        StringBuilder codeBuilder = new StringBuilder(baseLetter);
        SecureRandom secureRandom = new SecureRandom();


        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }

        return codeBuilder.toString();
    }

    private String generateUniqueChurchNumber(String churchName, int length) {
        String churchNumber;
        do {
            churchNumber = generateChurchNumber(churchName, length);
        } while (churchRepository.existsByChurchNumber(churchNumber)); // Keep generating if it already exists
        return churchNumber;
    }



}
