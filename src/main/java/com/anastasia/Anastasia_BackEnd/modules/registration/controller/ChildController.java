package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ChildStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registrar/children")
@RequiresTenantFeature(TenantFeature.MEMBER_MANAGEMENT)
public class ChildController {

    private final ChildService childService;

    @PreAuthorize("isAuthenticated() or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'ADD_MEMBERS')")
    @PostMapping("/register-child")
    public ResponseEntity<Child_MemberResponse> registerChild(@Valid @RequestBody Child_MemberDTO childMemberDTO){

        Child_MemberEntity childMemberEntity = childService.convertToEntity(childMemberDTO);
        childMemberEntity.setStatus(ChildStatus.PENDING.name());
        Child_MemberResponse response = childService.registerChild(childMemberEntity);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping
    public ResponseEntity<Page<Child_MemberSummaryResponse>> listOfChildren(
            Pageable pageable,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String language
    ){
        return new ResponseEntity<>(childService.findAllSummary(pageable, language), HttpStatus.OK);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping("/requests")
    public ResponseEntity<Page<Child_MemberResponse>> listPendingChildren(Pageable pageable){
        return new ResponseEntity<>(childService.findPending(pageable), HttpStatus.OK);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping("/count")
    public ResponseEntity<Long> countChildren() {
        return ResponseEntity.ok(childService.countNonPending());
    }


    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping("/search")
    public ResponseEntity<Page<Child_MemberSummaryResponse>> searchChildren(
            Pageable pageable,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "lang", required = false, defaultValue = "en") String language
    ){
        return new ResponseEntity<>(childService.searchNonPendingSummary(pageable, query, language), HttpStatus.OK);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping("/{memberId}")
    public ResponseEntity<Child_MemberResponse> getChild(@PathVariable Long memberId){
        return childService.findChildById(memberId).map(childMemberResponse ->
                new ResponseEntity<>(childMemberResponse, HttpStatus.OK)
        ).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'EDIT_CHILDREN')")
    @PatchMapping("/{memberId}")
    public ResponseEntity<Child_MemberResponse> updateMembershipDetails(@PathVariable Long memberId, @RequestBody Child_MemberDTO request){
        return ResponseEntity.ok(childService.updateChildDetails(memberId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'APPROVE_MEMBERSHIP')")
    @PatchMapping("/{memberId}/church-approve")
    public ResponseEntity<Child_MemberResponse> approveByChurch(@PathVariable Long memberId){
        Child_MemberResponse response = childService.approveByChurch(memberId);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'APPROVE_MEMBERSHIP_AS_PRIEST')")
    @PatchMapping("/{memberId}/priest-approve")
    public ResponseEntity<Child_MemberResponse> approveByPriest(@PathVariable Long memberId){
        Child_MemberResponse response = childService.approveByPriest(memberId);
        return ResponseEntity.ok(response);
    }


    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'DELETE_CHILDREN')")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> deleteMemberShip(@PathVariable Long memberId){
        childService.deleteChildMembership(memberId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'ADVANCED_SEARCH_MEMBERS')")
    @PostMapping("/advanced-search")
    public ResponseEntity<Page<Child_MemberResponse>> searchChildren(
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

        return new ResponseEntity<>(childService.findAllBySpecification(spec, pageable), HttpStatus.OK);
    }



}
