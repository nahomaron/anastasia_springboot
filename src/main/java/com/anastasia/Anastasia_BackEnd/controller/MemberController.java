package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberResponse;
import com.anastasia.Anastasia_BackEnd.model.member.MemberStatus;
import com.anastasia.Anastasia_BackEnd.service.registration.MemberService;
import com.anastasia.Anastasia_BackEnd.specification.MemberSpecifications;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registrar/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register-member")
    public ResponseEntity<MemberResponse> registerMember(@Valid @RequestBody MemberDTO memberDTO){

        MemberEntity memberEntity = memberService.convertToEntity(memberDTO);
        memberEntity.setStatus(MemberStatus.PENDING.name());
        MemberResponse response = memberService.registerMember(memberEntity);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<MemberDTO>> listOfMembers(Pageable pageable){
        Page<MemberEntity> members = memberService.findAll(pageable);
        return new ResponseEntity<>(
                members.map(memberService::convertToDTO), HttpStatus.OK);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberDTO> getMember(@PathVariable Long memberId){
        Optional<MemberEntity> foundMember = memberService.findMemberById(memberId);
        return foundMember.map(memberEntity -> {
            MemberDTO memberDTO = memberService.convertToDTO(memberEntity);
            return new ResponseEntity<>(memberDTO, HttpStatus.FOUND);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<?> updateMembershipDetails(@PathVariable Long memberId, @RequestBody MemberDTO request){
        memberService.updateMembershipDetails(memberId, request);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

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

    @PostMapping("/advanced-search")
    public ResponseEntity<Page<MemberDTO>> searchMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @RequestParam(required = false) Long membershipNumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) boolean deacon,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String motherName,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) int minAge,
            @RequestParam(required = false) int maxAge,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String maritalStatus,
            @RequestParam(required = false) String profession,
            @RequestParam(required = false) String levelOfEducation,
            @RequestBody(required = false) Address address
    ) {
        Specification<MemberEntity> spec = Specification.where(null);

        if (membershipNumber != null) {
            spec = spec.and(MemberSpecifications.hasMembershipNumber(membershipNumber));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and(MemberSpecifications.hasStatus(status));
        }
        if (Boolean.TRUE.equals(deacon)) {
            spec = spec.and(MemberSpecifications.isDeacon(true));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and(MemberSpecifications.nameContains(name));
        }
        if (motherName != null && !motherName.isBlank()) {
            spec = spec.and(MemberSpecifications.motherNameContains(motherName));
        }
        if (gender != null && !gender.isBlank()) {
            spec = spec.and(MemberSpecifications.hasGender(gender));
        }
        if (minAge != 0 && maxAge >= minAge) {
            spec = spec.and(MemberSpecifications.ageBetween(minAge, maxAge));
        }
        if (phone != null && !phone.isBlank()) {
            spec = spec.and(MemberSpecifications.phoneContains(phone));
        }
        if (maritalStatus != null && !maritalStatus.isBlank()) {
            spec = spec.and(MemberSpecifications.hasMaritalStatus(maritalStatus));
        }
        if (profession != null && !profession.isBlank()) {
            spec = spec.and(MemberSpecifications.hasProfession(profession));
        }
        if (levelOfEducation != null && !levelOfEducation.isBlank()) {
            spec = spec.and(MemberSpecifications.hasLevelOfEducation(levelOfEducation));
        }
        if (address != null) {
            spec = spec.and(MemberSpecifications.filterByAddress(address));
        }

//        Sort sortOrder = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Sort sortOrder = Sort.by("firstName").descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<MemberEntity> members = memberService.findAllBySpecification(spec, pageable);

        return new ResponseEntity<>(members.map(
                memberService::convertToDTO), HttpStatus.OK);
    }


}
