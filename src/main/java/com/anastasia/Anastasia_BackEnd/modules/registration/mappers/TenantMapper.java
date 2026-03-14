package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TenantMapper {

    @Mapping(target = "subscriptionPlan", source = "subscription.plan")
    @Mapping(target = "ownerEmail", source = "ownerEmail")
    TenantDTO tenantEntityToDTO(TenantEntity tenantEntity);

    @Mapping(target = "subscription", ignore = true)
    @Mapping(target = "church", ignore = true)
    @Mapping(target = "adminAssignments", ignore = true)
    TenantEntity tenantDTOToEntity(TenantDTO tenantDTO);

}
