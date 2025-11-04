package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.MemberStatus;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import com.anastasia.Anastasia_BackEnd.common.specification.MemberSpecifications;
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
@RequestMapping("/api/v1/registrar/members")
public class MemberController {

    private final MemberService memberService;

    //
    @PreAuthorize("hasAnyRole('USER') or hasAuthority('ADD_MEMBERS')")
    @PostMapping("/register-member")
    public ResponseEntity<MemberResponse> registerMember(@Valid @RequestBody Adult_MemberDTO adultMemberDTO){

        Adult_MemberEntity adultMemberEntity = memberService.convertToEntity(adultMemberDTO);
        adultMemberEntity.setStatus(MemberStatus.PENDING.name());
        MemberResponse response = memberService.registerMember(adultMemberEntity);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('PRIEST', 'OWNER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_MEMBERS')")
    @GetMapping
    public ResponseEntity<Page<Adult_MemberDTO>> listOfMembers(Pageable pageable){
        Page<Adult_MemberEntity> members = memberService.findAll(pageable);
        return new ResponseEntity<>(
                members.map(memberService::convertToDTO), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('PRIEST', 'OWNER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_MEMBERS')")
    @GetMapping("/{memberId}")
    public ResponseEntity<Adult_MemberDTO> getMember(@PathVariable Long memberId){
        Optional<Adult_MemberEntity> foundMember = memberService.findMemberById(memberId);
        return foundMember.map(memberEntity -> {
            Adult_MemberDTO adultMemberDTO = memberService.convertToDTO(memberEntity);
            return new ResponseEntity<>(adultMemberDTO, HttpStatus.FOUND);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PreAuthorize("hasAnyRole('PRIEST', 'OWNER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'EDIT_MEMBERS')")
    @PatchMapping("/{memberId}")
    public ResponseEntity<?> updateMembershipDetails(@PathVariable Long memberId, @RequestBody Adult_MemberDTO request){
        memberService.updateMembershipDetails(memberId, request);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasAnyRole('PRIEST', 'OWNER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'APPROVE_MEMBERSHIP')")
    @PatchMapping("/{memberId}/church-approve")
    public ResponseEntity<?> approveByChurch(@PathVariable Long memberId){
        memberService.approveByChurch(memberId);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasRole('PRIEST')")
    @PatchMapping("/{memberId}/priest-approve")
    public ResponseEntity<?> approveByPriest(@PathVariable Long memberId){
        memberService.approveByPriest(memberId);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasAnyRole('OWNER') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'DELETE_MEMBERS')")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> deleteMemberShip(@PathVariable Long memberId){
        memberService.deleteMembership(memberId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

//    @GetMapping("/search")
//    public List<Product> searchProducts(
//            @RequestParam(required = false) String category,
//            @RequestParam(required = false) String keyword,
//            @RequestParam(required = false) Double minPrice,
//            @RequestParam(required = false) Double maxPrice,
//            @RequestParam(required = false) Boolean available,
//            @RequestBody(required = false) Address address
//            ) {
//        Specification<Product> spec = Specification.where(null);
//
//        if (category != null) {
//            spec = spec.and(ProductSpecifications.hasCategory(category));
//        }
//        if (keyword != null) {
//            spec = spec.and(ProductSpecifications.nameContains(keyword));
//        }
//        if (minPrice != null && maxPrice != null) {
//            spec = spec.and(ProductSpecifications.priceBetween(minPrice, maxPrice));
//        }
//        if (available != null) {
//            spec = spec.and(ProductSpecifications.isAvailable(available));
//        }
//
//        return productRepository.findAll(spec);
//    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'ADVANCED_SEARCH_MEMBERS')")
    @PostMapping("/advanced-search")
    public ResponseEntity<Page<Adult_MemberDTO>> searchMembers(
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
            @RequestParam(required = false) String maritalStatus,
            @RequestParam(required = false) String profession,
            @RequestParam(required = false) String levelOfEducation,
            @RequestBody(required = false) Address address
    ) {
        List<Specification<Adult_MemberEntity>> specs = new ArrayList<>();

        if (membershipNumber != null) {
            specs.add(MemberSpecifications.hasMembershipNumber(membershipNumber));
        }
        if (status != null && !status.isBlank()) {
            specs.add(MemberSpecifications.hasStatus(status));
        }
        if (Boolean.TRUE.equals(deacon)) {
            specs.add(MemberSpecifications.isDeacon(true));
        }
        if (name != null && !name.isBlank()) {
            specs.add(MemberSpecifications.nameContains(name));
        }
        if (motherName != null && !motherName.isBlank()) {
            specs.add(MemberSpecifications.motherNameContains(motherName));
        }
        if (gender != null && !gender.isBlank()) {
            specs.add(MemberSpecifications.hasGender(gender));
        }
        if (minAge != null && maxAge >= minAge) {
            specs.add(MemberSpecifications.ageBetween(minAge, maxAge));
        }
        if (phone != null && !phone.isBlank()) {
            specs.add(MemberSpecifications.phoneContains(phone));
        }
        if (maritalStatus != null && !maritalStatus.isBlank()) {
            specs.add(MemberSpecifications.hasMaritalStatus(maritalStatus));
        }
        if (profession != null && !profession.isBlank()) {
            specs.add(MemberSpecifications.hasProfession(profession));
        }
        if (levelOfEducation != null && !levelOfEducation.isBlank()) {
            specs.add(MemberSpecifications.hasLevelOfEducation(levelOfEducation));
        }
        if (address != null) {
            specs.add(MemberSpecifications.filterByAddress(address));
        }

        Specification<Adult_MemberEntity> spec = specs.stream()
                .reduce(Specification::and)
                .orElse(null);

//        Sort sortOrder = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Sort sortOrder = Sort.by("firstName").descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<Adult_MemberEntity> members = memberService.findAllBySpecification(spec, pageable);

        return new ResponseEntity<>(members.map(
                memberService::convertToDTO), HttpStatus.OK);
    }



}
