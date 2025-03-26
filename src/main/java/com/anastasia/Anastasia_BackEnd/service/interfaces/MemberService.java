package com.anastasia.Anastasia_BackEnd.service.interfaces;

import com.anastasia.Anastasia_BackEnd.model.DTO.membership.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.membership.MemberEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MemberService {
    MemberEntity convertToEntity(MemberDTO memberDTO);

    MemberEntity registerMember(MemberEntity memberEntity);

    MemberDTO convertToDTO(MemberEntity savedMember);

    List<MemberEntity> findAll();
}
