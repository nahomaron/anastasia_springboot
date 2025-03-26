package com.anastasia.Anastasia_BackEnd.controller.registration;

import com.anastasia.Anastasia_BackEnd.model.DTO.membership.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.entity.membership.MemberEntity;
import com.anastasia.Anastasia_BackEnd.service.interfaces.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registrar")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register-member")
    public ResponseEntity<MemberDTO> registerMember(@RequestBody MemberDTO memberDTO){
        MemberEntity memberEntity = memberService.convertToEntity(memberDTO);
        MemberEntity savedMember = memberService.registerMember(memberEntity);
        return new ResponseEntity<>(memberService.convertToDTO(savedMember), HttpStatus.CREATED);
    }

    @GetMapping("/memberships")
    public ResponseEntity<List<MemberDTO>> listOfMembers(){
        List<MemberEntity> members = memberService.findAll();
        return new ResponseEntity<>(
                members.stream()
                        .map(memberService::convertToDTO)
                        .toList(), HttpStatus.OK);
    }

}
