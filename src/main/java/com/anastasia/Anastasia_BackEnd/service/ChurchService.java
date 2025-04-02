package com.anastasia.Anastasia_BackEnd.service;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

@Service
public interface ChurchService {
    ChurchEntity convertToEntity(ChurchDTO churchDTO);

    ChurchEntity createChurch(ChurchEntity churchEntity);

    ChurchDTO convertToDTO(ChurchEntity churchEntity);
}
