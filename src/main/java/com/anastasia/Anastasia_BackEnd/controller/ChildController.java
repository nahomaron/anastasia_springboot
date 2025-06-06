package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.child.ChildResponse;
import com.anastasia.Anastasia_BackEnd.model.child.ChildStatus;
import com.anastasia.Anastasia_BackEnd.model.common.Address;
import com.anastasia.Anastasia_BackEnd.model.child.ChildDTO;
import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import com.anastasia.Anastasia_BackEnd.service.registration.ChildService;
import com.anastasia.Anastasia_BackEnd.specification.ChildSpecifications;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registrar/children")
public class ChildController {

    private final ChildService childService;

    @PostMapping("/register-child")
    public ResponseEntity<ChildResponse> registerChild(@Valid @RequestBody ChildDTO childDTO){

        ChildEntity childEntity = childService.convertToEntity(childDTO);
        childEntity.setStatus(ChildStatus.PENDING.name());
        ChildResponse response = childService.registerChild(childEntity);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<ChildDTO>> listOfChildren(Pageable pageable){
        Page<ChildEntity> children = childService.findAll(pageable);
        return new ResponseEntity<>(
                children.map(childService::convertToDTO), HttpStatus.OK);
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ChildDTO> getChild(@PathVariable Long memberId){
        Optional<ChildEntity> foundChild = childService.findChildById(memberId);
        return foundChild.map(childEntity -> {
            ChildDTO childDTO = childService.convertToDTO(childEntity);
            return new ResponseEntity<>(childDTO, HttpStatus.FOUND);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<?> updateMembershipDetails(@PathVariable Long memberId, @RequestBody ChildDTO request){
        childService.updateChildDetails(memberId, request);
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }


    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> deleteMemberShip(@PathVariable Long memberId){
        childService.deleteChildMembership(memberId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/advanced-search")
    public ResponseEntity<Page<ChildDTO>> searchChildren(
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
        Specification<ChildEntity> spec = Specification.where(null);

        if (membershipNumber != null) {
            spec = spec.and(ChildSpecifications.hasMembershipNumber(membershipNumber));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and(ChildSpecifications.hasStatus(status));
        }
        if (Boolean.TRUE.equals(deacon)) {
            spec = spec.and(ChildSpecifications.isDeacon(true));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and(ChildSpecifications.nameContains(name));
        }
        if (motherName != null && !motherName.isBlank()) {
            spec = spec.and(ChildSpecifications.motherNameContains(motherName));
        }
        if (gender != null && !gender.isBlank()) {
            spec = spec.and(ChildSpecifications.hasGender(gender));
        }
        if (minAge != null && maxAge >= minAge) {
            spec = spec.and(ChildSpecifications.ageBetween(minAge, maxAge));
        }
        if (phone != null && !phone.isBlank()) {
            spec = spec.and(ChildSpecifications.phoneContains(phone));
        }
        if (levelOfEducation != null && !levelOfEducation.isBlank()) {
            spec = spec.and(ChildSpecifications.hasLevelOfEducation(levelOfEducation));
        }
        if (address != null) {
            spec = spec.and(ChildSpecifications.filterByAddress(address));
        }

//        Sort sortOrder = Sort.by(Sort.Direction.fromString(sort[1]), sort[0]);
        Sort sortOrder = Sort.by("firstName").descending();

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<ChildEntity> members = childService.findAllBySpecification(spec, pageable);

        return new ResponseEntity<>(members.map(
                childService::convertToDTO), HttpStatus.OK);
    }



}
