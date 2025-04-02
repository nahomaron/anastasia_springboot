package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MemberService {
    MemberEntity convertToEntity(MemberDTO memberDTO);

    MemberEntity registerMember(MemberEntity memberEntity);

    MemberDTO convertToDTO(MemberEntity savedMember);

    List<MemberEntity> findAll();
}
