package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildService;
import com.anastasia.Anastasia_BackEnd.common.specification.ChildSpecifications;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registrar/children")
public class ChildController {

    private final ChildService childService;

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIEST') or " +
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'ADD_MEMBERS')")
    @PostMapping("/register-child")
    public ResponseEntity<ChildResponse> registerChild(@Valid @RequestBody Child_MemberDTO childMemberDTO){

        Child_MemberEntity childMemberEntity = childService.convertToEntity(childMemberDTO);
        childMemberEntity.setStatus(ChildStatus.PENDING.name());
        ChildResponse response = childService.registerChild(childMemberEntity);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIEST') or " +
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping
    public ResponseEntity<Page<Child_MemberDTO>> listOfChildren(Pageable pageable){
        Page<Child_MemberEntity> children = childService.findAll(pageable);
        return new ResponseEntity<>(
                children.map(childService::convertToDTO), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIEST') or " +
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping("/{memberId}")
    public ResponseEntity<Child_MemberDTO> getChild(@PathVariable Long memberId){
        Optional<Child_MemberEntity> foundChild = childService.findChildById(memberId);
        return foundChild.map(childEntity -> {
            Child_MemberDTO childMemberDTO = childService.convertToDTO(childEntity);
            return new ResponseEntity<>(childMemberDTO, HttpStatus.FOUND);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIEST') or " +
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'EDIT_CHILDREN')")
    @PatchMapping("/{memberId}")
    public ResponseEntity<?> updateMembershipDetails(@PathVariable Long memberId, @RequestBody Child_MemberDTO request){
        childService.updateChildDetails(memberId, request);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }


    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIEST') or " +
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'DELETE_CHILDREN')")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> deleteMemberShip(@PathVariable Long memberId){
        childService.deleteChildMembership(memberId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OWNER', 'PRIEST') OR " +
            "@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'ADVANCED_SEARCH_MEMBERS')")
    @PostMapping("/advanced-search")
    public ResponseEntity<Page<Child_MemberDTO>> searchChildren(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @RequestParam(required = false) Long membershipNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) boolean deacon,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String motherName,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String levelOfEducation,
            @RequestBody(required = false) Address address
    ) {
        List<Specification<Child_MemberEntity>> specs = new ArrayList<>();

        if (membershipNumber != null) {
            specs.add(ChildSpecifications.hasMembershipNumber(membershipNumber));
        }
        if (status != null && !status.isBlank()) {
            specs.add(ChildSpecifications.hasStatus(status));
        }
        if (Boolean.TRUE.equals(deacon)) {
            specs.add(ChildSpecifications.isDeacon(true));
        }
        if (name != null && !name.isBlank()) {
            specs.add(ChildSpecifications.nameContains(name));
        }
        if (motherName != null && !motherName.isBlank()) {
            specs.add(ChildSpecifications.motherNameContains(motherName));
        }
        if (gender != null && !gender.isBlank()) {
            specs.add(ChildSpecifications.hasGender(gender));
        }
        if (minAge != null && maxAge >= minAge) {
            specs.add(ChildSpecifications.ageBetween(minAge, maxAge));
        }
        if (phone != null && !phone.isBlank()) {
            specs.add(ChildSpecifications.phoneContains(phone));
        }
        if (levelOfEducation != null && !levelOfEducation.isBlank()) {
            specs.add(ChildSpecifications.hasLevelOfEducation(levelOfEducation));
        }
        if (address != null) {
            specs.add(ChildSpecifications.filterByAddress(address));
        }

        Specification<Child_MemberEntity> spec = specs.stream()
                .reduce(Specification::and)
                .orElse(null);

//        Sort sortOrder = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Sort sortOrder = Sort.by("firstName").descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<Child_MemberEntity> members = childService.findAllBySpecification(spec, pageable);

        return new ResponseEntity<>(members.map(
                childService::convertToDTO), HttpStatus.OK);
    }



}
