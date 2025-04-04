package com.anastasia.Anastasia_BackEnd.controller;

import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import com.anastasia.Anastasia_BackEnd.model.member.MemberResponse;
import com.anastasia.Anastasia_BackEnd.model.member.MemberStatus;
import com.anastasia.Anastasia_BackEnd.service.registration.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}
