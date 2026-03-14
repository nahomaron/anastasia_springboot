package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserDTO;
import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/*
 Since we don't want to expose the entity to any external transactions through controller, we use mapper
 to expose only the DTO
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true)
)
public interface UsersMapper {

    UserDTO userEntityToUserDTO(UserEntity userEntity);

    UserEntity userDTOToUserEntity(UserDTO userDTO);
}
