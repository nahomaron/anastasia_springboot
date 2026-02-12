package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
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

    Page<Child_MemberEntity> findAll(Pageable pageable);

    long countNonPending();

    Page<Child_MemberResponse> findByTenantAndPriestNumber(UUID tenantId, String priestNumber, Pageable pageable);

    Page<Child_MemberEntity> findPending(Pageable pageable);

    Page<Child_MemberEntity> searchNonPending(Pageable pageable, String query);

    Optional<Child_MemberEntity> findChildById(Long memberId);

    void updateChildDetails(Long memberId, Child_MemberDTO request);

    void deleteChildMembership(Long memberId);

    Page<Child_MemberEntity> findAllBySpecification(Specification<Child_MemberEntity> spec, Pageable pageable);

}
