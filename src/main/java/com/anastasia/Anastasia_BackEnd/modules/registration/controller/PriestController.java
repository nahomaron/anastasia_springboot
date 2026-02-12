package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.ChildService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.PriestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/priests")
public class PriestController {

    private final PriestService priestService;
    private final MemberService memberService;
    private final ChildService childService;

    @PostMapping("/register")
    public ResponseEntity<?> registerPriest(@Valid @RequestBody PriestDTO priestDTO){

        if(!priestDTO.isPasswordMatch()){
            return ResponseEntity.badRequest().body("Password do not match");
        }
        priestService.registerPriest(priestDTO);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<PriestDTO>> listOfPriests(Pageable pageable){
        Page<PriestEntity> priests = priestService.findAllPriests(pageable);
        return new ResponseEntity<>(priests.map(priestService::convertToDTO), HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN', 'PLATFORM_ADMIN')")
    @GetMapping("/church/{churchId}")
    public ResponseEntity<List<PriestDTO>> listPriestsByChurch(@PathVariable Long churchId) {
        List<PriestEntity> priests = priestService.findPriestsByChurchId(churchId);
        List<PriestDTO> dtos = priests.stream()
                .map(priestService::convertToDTO)
                .toList();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PLATFORM_ADMIN')")
    @GetMapping("/{priestId}")
    public ResponseEntity<PriestDTO> getPriest(@PathVariable Long priestId){
        Optional<PriestEntity> foundPriest = priestService.findPriestById(priestId);

        return foundPriest.map(priestEntity -> {
            PriestDTO priestDTO = priestService.convertToDTO(priestEntity);
            return new ResponseEntity<>(priestDTO, HttpStatus.FOUND);
        }).orElse(
                new ResponseEntity<>(HttpStatus.NOT_FOUND)
        );
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PLATFORM_ADMIN', 'PRIEST')")
    @PatchMapping("/{priestId}")
    public ResponseEntity<PriestDTO> updatePriestDetails(@PathVariable Long priestId,
                                                         @RequestBody PriestDTO priestDTO){
        PriestEntity priestEntity = priestService.convertToEntity(priestDTO);
        PriestEntity updatedPriest = priestService.updatePriestDetails(priestId, priestEntity);
        return new ResponseEntity<>(priestService.convertToDTO(updatedPriest), HttpStatus.ACCEPTED);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PLATFORM_ADMIN')")
    @PostMapping("/delete/{priestId}")
    public ResponseEntity<?> deletePriest(@PathVariable Long priestId){
        priestService.deletePriest(priestId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('PRIEST', 'OWNER', 'ADMIN', 'PLATFORM_ADMIN') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_MEMBERS')")
    @GetMapping("/{priestNumber}/members")
    public ResponseEntity<Page<Adult_MemberResponse>> listMembersByPriest(
            @PathVariable String priestNumber,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String status,
            Pageable pageable
    ) {
        UUID effectiveTenantId = resolveTenantId(tenantId);
        Page<Adult_MemberResponse> members = status == null || status.isBlank()
                ? memberService.findByTenantAndPriestNumber(effectiveTenantId, priestNumber, pageable)
                : memberService.findByTenantAndPriestNumberAndStatus(effectiveTenantId, priestNumber, status, pageable);
        return new ResponseEntity<>(members, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('PRIEST', 'OWNER', 'ADMIN', 'PLATFORM_ADMIN') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_MEMBERS')")
    @GetMapping("/{priestNumber}/members/pending")
    public ResponseEntity<Page<Adult_MemberResponse>> listPendingMembersByPriest(
            @PathVariable String priestNumber,
            @RequestParam(required = false) UUID tenantId,
            Pageable pageable
    ) {
        UUID effectiveTenantId = resolveTenantId(tenantId);
        Page<Adult_MemberResponse> members = memberService.findPendingByTenantAndPriestNumber(
                effectiveTenantId,
                priestNumber,
                pageable
        );
        return new ResponseEntity<>(members, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('PRIEST', 'OWNER', 'ADMIN', 'PLATFORM_ADMIN') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_CHILDREN')")
    @GetMapping("/{priestNumber}/children")
    public ResponseEntity<Page<Child_MemberResponse>> listChildrenByPriest(
            @PathVariable String priestNumber,
            @RequestParam(required = false) UUID tenantId,
            Pageable pageable
    ) {
        UUID effectiveTenantId = resolveTenantId(tenantId);
        Page<Child_MemberResponse> children = childService.findByTenantAndPriestNumber(
                effectiveTenantId,
                priestNumber,
                pageable
        );
        return new ResponseEntity<>(children, HttpStatus.OK);
    }

    private UUID resolveTenantId(UUID tenantId) {
        UUID effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId();
        if (effectiveTenantId == null) {
            throw new IllegalStateException("Tenant id is required");
        }
        return effectiveTenantId;
    }
}
