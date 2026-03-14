package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.family.UpdateFamilyRelationshipRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.family.UpsertFamilyRelationshipRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.FamilyMemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family.MyFamilyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public interface MemberService {
    Adult_MemberEntity convertToEntity(Adult_MemberDTO adultMemberDTO);

    Adult_MemberResponse registerMember(Adult_MemberEntity adultMemberEntity);

    Adult_MemberDTO convertToDTO(Adult_MemberEntity savedMember);

    Adult_MemberResponse convertToResponse(Adult_MemberEntity adultMemberEntity);

    Page<Adult_MemberResponse> findAll(Pageable pageable);
    Page<Adult_MemberSummaryResponse> findAllSummary(Pageable pageable);

    long countNonPending();

    Page<Adult_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);
    Page<Adult_MemberSummaryResponse> findByTenantAndPriestNumberSummary(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Adult_MemberResponse> findByTenantAndPriestNumberAndStatus(UUID tenantId, String priestNumber, String status, Pageable pageable);
    Page<Adult_MemberSummaryResponse> findByTenantAndPriestNumberAndStatusSummary(UUID tenantId, String priestNumber, String status, Pageable pageable);

    Page<Adult_MemberResponse> findPending(Pageable pageable);

    Page<Adult_MemberResponse> findPendingByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Adult_MemberResponse> searchNonPending(Pageable pageable, String query);
    Page<Adult_MemberSummaryResponse> searchNonPendingSummary(Pageable pageable, String query);

    Optional<Adult_MemberResponse> findMemberById(Long memberId);

    Adult_MemberResponse updateMembershipDetails(Long memberId, Adult_MemberDTO request);

    void deleteMembership(Long memberId);

    Adult_MemberResponse approveByChurch(Long memberId);

    Adult_MemberResponse approveByPriest(Long memberId);

    Page<Adult_MemberResponse> findAllBySpecification(Specification<Adult_MemberEntity> spec, Pageable pageable);

    MyFamilyResponse getCurrentUserFamily();

    FamilyMemberSummaryResponse createFamilyRelationship(UpsertFamilyRelationshipRequest request);

    FamilyMemberSummaryResponse updateFamilyRelationship(Long relationshipId, UpdateFamilyRelationshipRequest request);

    void deleteFamilyRelationship(Long relationshipId);
}
