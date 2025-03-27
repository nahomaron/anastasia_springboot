package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.DTO.TenantDetailsDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.embeded.TenantDetails;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantDetailsMapper {

    TenantDetailsDTO tenantDetailsEntityToDTO(TenantDetails tenantDetails);

    TenantDetails tenantDetailsDTOToEntity(TenantDetailsDTO tenantDetailsDTO);

}
