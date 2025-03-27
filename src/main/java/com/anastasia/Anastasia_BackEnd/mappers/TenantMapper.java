package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.TenantEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    TenantDTO tenantEntityToDTO(TenantEntity tenantEntity);

    TenantEntity tenantDTOToEntity(TenantDTO tenantDTO);

}
