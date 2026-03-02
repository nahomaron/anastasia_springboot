package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public interface ChildService {
    Child_MemberEntity convertToEntity(@Valid Child_MemberDTO childMemberDTO);
    Child_MemberDTO convertToDTO(Child_MemberEntity childMemberEntity);
    Child_MemberResponse convertToResponse(Child_MemberEntity childMemberEntity);

    ChildResponse registerChild(Child_MemberEntity childMemberEntity);

    Page<Child_MemberResponse> findAll(Pageable pageable);
    Page<Child_MemberSummaryResponse> findAllSummary(Pageable pageable);

    long countNonPending();

    Page<Child_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);
    Page<Child_MemberSummaryResponse> findByTenantAndPriestNumberSummary(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Child_MemberResponse> findPending(Pageable pageable);

    Page<Child_MemberResponse> searchNonPending(Pageable pageable, String query);
    Page<Child_MemberSummaryResponse> searchNonPendingSummary(Pageable pageable, String query);

    Optional<Child_MemberResponse> findChildById(Long memberId);

    void updateChildDetails(Long memberId, Child_MemberDTO request);

    void deleteChildMembership(Long memberId);

    Page<Child_MemberResponse> findAllBySpecification(Specification<Child_MemberEntity> spec, Pageable pageable);

    Child_MemberResponse approveByChurch(Long memberId);

    Child_MemberResponse approveByPriest(Long memberId);

}
