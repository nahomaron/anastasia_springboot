package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface MemberService {
    Adult_MemberEntity convertToEntity(Adult_MemberDTO adultMemberDTO);

    MemberResponse registerMember(Adult_MemberEntity adultMemberEntity);

    Adult_MemberDTO convertToDTO(Adult_MemberEntity savedMember);

    Page<Adult_MemberEntity> findAll(Pageable pageable);

    Optional<Adult_MemberEntity> findMemberById(Long memberId);

    void updateMembershipDetails(Long memberId, Adult_MemberDTO request);

    void deleteMembership(Long memberId);

    void approveByChurch(Long memberId);

    void approveByPriest(Long memberId);

    Page<Adult_MemberEntity> findAllBySpecification(Specification<Adult_MemberEntity> spec, Pageable pageable);
}
