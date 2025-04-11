package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface ChurchService {
    ChurchEntity convertToEntity(ChurchDTO churchDTO);

    String createChurch(ChurchEntity churchEntity);

    ChurchDTO convertToDTO(ChurchEntity churchEntity);

    Page<ChurchEntity> findAll(Pageable pageable);
}
