package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.PublicChurchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface ChurchService {
    ChurchEntity convertToEntity(ChurchDTO churchDTO);

    ChurchResponse createChurch(ChurchEntity churchEntity);

    ChurchDTO convertToDTO(ChurchEntity churchEntity);

    ChurchResponse convertToResponse(ChurchEntity churchEntity);

    PublicChurchResponse convertToPublicResponse(ChurchEntity churchEntity);

    Page<ChurchResponse> findAll(Pageable pageable, String query, Boolean usesOurServices);

    Page<PublicChurchResponse> findAllPublic(Pageable pageable, String query, Boolean usesOurServices);

    boolean exists(Long churchId);

    ChurchResponse updateChurch(Long churchId, ChurchEntity churchEntity);

    void deleteChurch(Long churchId);

    Optional<ChurchEntity> findOne(Long churchId);

    Optional<ChurchEntity> findOneByChurchNumber(String churchNumber);

    Optional<ChurchEntity> findOneByChurchNumberUsingOurServices(String churchNumber);

    Optional<ChurchEntity> findOnePublicByChurchNumber(String churchNumber);

    Optional<ChurchEntity> findOnePublicByChurchNumberUsingOurServices(String churchNumber);
}
