package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.user.UserDTO;
import com.anastasia.Anastasia_BackEnd.model.user.UserEntity;
import org.mapstruct.Mapper;

/*
 Since we don't want to expose the entity to any external transactions through controller, we use mapper
 to expose only the DTO
 */
@Mapper(componentModel = "spring")
public interface UsersMapper {

    UserDTO userEntityToUserDTO(UserEntity userEntity);

    UserEntity userDTOToUserEntity(UserDTO userDTO);
}
