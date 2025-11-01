package com.anastasia.Anastasia_BackEnd.modules.registration.service;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.child.ChildResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface ChildService {
    ChildEntity convertToEntity(@Valid ChildDTO childDTO);
    ChildDTO convertToDTO(ChildEntity childEntity);

    ChildResponse registerChild(ChildEntity childEntity);

    Page<ChildEntity> findAll(Pageable pageable);

    Optional<ChildEntity> findChildById(Long memberId);

    void updateChildDetails(Long memberId, ChildDTO request);

    void deleteChildMembership(Long memberId);

    Page<ChildEntity> findAllBySpecification(Specification<ChildEntity> spec, Pageable pageable);

}
