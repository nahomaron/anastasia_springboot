package com.anastasia.Anastasia_BackEnd.service.registration;

import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public interface MemberService {
    MemberEntity convertToEntity(MemberDTO memberDTO);

    MemberResponse registerMember(MemberEntity memberEntity);

    MemberDTO convertToDTO(MemberEntity savedMember);

    Page<MemberEntity> findAll(Pageable pageable);

    Optional<MemberEntity> findMemberById(Long memberId);

    void updateMembershipDetails(Long memberId, MemberDTO request);

    void deleteMembership(Long memberId);

    void approveByChurch(Long memberId);

    void approveByPriest(Long memberId);

    Page<MemberEntity> findAllBySpecification(Specification<MemberEntity> spec, Pageable pageable);
}
