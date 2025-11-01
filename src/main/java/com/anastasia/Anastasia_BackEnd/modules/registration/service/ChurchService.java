package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface ChurchService {
    ChurchEntity convertToEntity(ChurchDTO churchDTO);

    String createChurch(ChurchEntity churchEntity);

    ChurchDTO convertToDTO(ChurchEntity churchEntity);

    Page<ChurchEntity> findAll(Pageable pageable);

    boolean exists(Long churchId);

    void updateChurch(Long churchId, ChurchEntity churchEntity);

    void deleteChurch(Long churchId);

    Optional<ChurchEntity> findOne(Long churchId);
}
