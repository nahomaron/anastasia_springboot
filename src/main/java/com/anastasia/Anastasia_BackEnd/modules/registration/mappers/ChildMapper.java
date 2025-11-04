package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import org.springframework.stereotype.Component;

@Component
public class ChildMapper {

    public Child_MemberDTO childEntityToDTO(Child_MemberEntity childMemberEntity) {
        if (childMemberEntity == null) return null;

        return Child_MemberDTO.builder()
                .churchNumber(childMemberEntity.getChurchNumber())
                .deacon(childMemberEntity.isDeacon())
                .title(childMemberEntity.getTitle())
                .firstName(childMemberEntity.getFirstName())
                .fatherName(childMemberEntity.getFatherName())
                .grandFatherName(childMemberEntity.getGrandFatherName())
                .motherName(childMemberEntity.getMotherName())
                .mothersFather(childMemberEntity.getMothersFather())
                .firstNameT(childMemberEntity.getFirstNameT())
                .fatherNameT(childMemberEntity.getFatherNameT())
                .grandFatherNameT(childMemberEntity.getGrandFatherNameT())
                .motherFullNameT(childMemberEntity.getMotherFullNameT())
                .gender(childMemberEntity.getGender())
                .birthday(childMemberEntity.getBirthday())
                .nationality(childMemberEntity.getNationality())
                .placeOfBirth(childMemberEntity.getPlaceOfBirth())
                .email(childMemberEntity.getEmail())
                .phone(childMemberEntity.getPhone())
                .whatsApp(childMemberEntity.getWhatsApp())
                .emergencyContactNumber(childMemberEntity.getEmergencyContactNumber())
                .contactRelation(childMemberEntity.getContactRelation())
                .firstLanguage(childMemberEntity.getFirstLanguage())
                .secondLanguage(childMemberEntity.getSecondLanguage())
                .levelOfEducation(childMemberEntity.getLevelOfEducation())
                .fatherOfConfession(childMemberEntity.getFatherOfConfession())
                .address(childMemberEntity.getAddress())
                .build();
    }

    public Child_MemberEntity childDTOToEntity(Child_MemberDTO dto) {
        if (dto == null) return null;

        return Child_MemberEntity.builder()
                .churchNumber(dto.getChurchNumber())
                .deacon(dto.isDeacon())
                .title(dto.getTitle())
                .firstName(dto.getFirstName())
                .fatherName(dto.getFatherName())
                .grandFatherName(dto.getGrandFatherName())
                .motherName(dto.getMotherName())
                .mothersFather(dto.getMothersFather())
                .firstNameT(dto.getFirstNameT())
                .fatherNameT(dto.getFatherNameT())
                .grandFatherNameT(dto.getGrandFatherNameT())
                .motherFullNameT(dto.getMotherFullNameT())
                .gender(dto.getGender())
                .birthday(dto.getBirthday())
                .nationality(dto.getNationality())
                .placeOfBirth(dto.getPlaceOfBirth())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .whatsApp(dto.getWhatsApp())
                .emergencyContactNumber(dto.getEmergencyContactNumber())
                .contactRelation(dto.getContactRelation())
                .firstLanguage(dto.getFirstLanguage())
                .secondLanguage(dto.getSecondLanguage())
                .levelOfEducation(dto.getLevelOfEducation())
                .fatherOfConfession(dto.getFatherOfConfession())
                .address(dto.getAddress())
                .build();
    }
}
