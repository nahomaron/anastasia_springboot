package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.mappers.MemberMapper;
import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.repository.registration.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final MemberRepository memberRepository;

    @Override
    public MemberEntity convertToEntity(MemberDTO memberDTO) {
        return memberMapper.memberDTOToEntity(memberDTO);
    }

    @Override
    public MemberDTO convertToDTO(MemberEntity memberEntity) {
        return memberMapper.memberEntityToDTO(memberEntity);
    }

    @Override
    public MemberEntity registerMember(MemberEntity memberEntity) {
        return memberRepository.save(memberEntity);
    }

    @Override
    public List<MemberEntity> findAll() {
        return memberRepository.findAll().stream().toList();
    }

}
