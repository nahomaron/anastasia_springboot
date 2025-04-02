package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.model.priest.PriestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface PriestService {

    PriestEntity convertToEntity(PriestDTO priestDTO);
    PriestDTO convertToDTO(PriestEntity registeredPriest);

    void registerPriest(PriestDTO priestDTO);

    Page<PriestEntity> findAllPriests(Pageable pageable);

    Optional<PriestEntity> findPriestById(Long priestId);

    PriestEntity updatePriestDetails(Long priestId, PriestEntity priestEntity);

    void deletePriest(Long priestId);
}
