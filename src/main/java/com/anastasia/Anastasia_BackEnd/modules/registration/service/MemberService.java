package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public interface MemberService {
    Adult_MemberEntity convertToEntity(Adult_MemberDTO adultMemberDTO);

    MemberResponse registerMember(Adult_MemberEntity adultMemberEntity);

    Adult_MemberDTO convertToDTO(Adult_MemberEntity savedMember);

    Adult_MemberResponse convertToResponse(Adult_MemberEntity adultMemberEntity);

    Page<Adult_MemberResponse> findAll(Pageable pageable);

    long countNonPending();

    Page<Adult_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Adult_MemberResponse> findByTenantAndPriestNumberAndStatus(UUID tenantId, String priestNumber, String status, Pageable pageable);

    Page<Adult_MemberEntity> findPending(Pageable pageable);

    Page<Adult_MemberResponse> findPendingByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Adult_MemberEntity> searchNonPending(Pageable pageable, String query);

    Optional<Adult_MemberEntity> findMemberById(Long memberId);

    void updateMembershipDetails(Long memberId, Adult_MemberDTO request);

    void deleteMembership(Long memberId);

    Adult_MemberResponse approveByChurch(Long memberId);

    Adult_MemberResponse approveByPriest(Long memberId);

    Page<Adult_MemberEntity> findAllBySpecification(Specification<Adult_MemberEntity> spec, Pageable pageable);
}
