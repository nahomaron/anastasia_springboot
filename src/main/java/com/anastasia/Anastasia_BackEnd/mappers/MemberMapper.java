package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    MemberDTO memberEntityToDTO(MemberEntity memberEntity);

    MemberEntity memberDTOToEntity(MemberDTO memberDTO);
}
