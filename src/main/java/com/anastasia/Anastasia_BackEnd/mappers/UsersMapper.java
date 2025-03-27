package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.DTO.auth.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.auth.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/*
 Since we don't want to expose the entity to any external transactions through controller, we use mapper
 to expose only the DTO
 */
@Mapper(componentModel = "spring", uses = TenantDetailsMapper.class)
public interface UsersMapper {

    @Mapping(source = "tenantDetails", target = "tenantDetails" )
    UserDTO userEntityToUserDTO(UserEntity userEntity);

    @Mapping(source = "tenantDetails", target = "tenantDetails")
    UserEntity userDTOToUserEntity(UserDTO userDTO);
}
