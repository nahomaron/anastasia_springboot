package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface PriestService {

    PriestEntity convertToEntity(PriestDTO priestDTO);
    PriestDTO convertToDTO(PriestEntity registeredPriest);
    PriestResponse convertToResponse(PriestEntity registeredPriest);

    void registerPriest(PriestDTO priestDTO);

    Page<PriestResponse> findAllPriests(Pageable pageable);

    Optional<PriestResponse> findPriestById(Long priestId);

    List<PriestResponse> findPriestsByChurchId(Long churchId);
    List<PriestResponse> findActivePriestsByChurchId(Long churchId);

    PriestResponse updatePriestDetails(Long priestId, PriestEntity priestEntity, Boolean isActive);

    void deletePriest(Long priestId);
}
