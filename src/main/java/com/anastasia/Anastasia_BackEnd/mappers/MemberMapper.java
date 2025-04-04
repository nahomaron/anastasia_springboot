package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.member.MemberDTO;
import com.anastasia.Anastasia_BackEnd.model.member.MemberEntity;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public MemberDTO memberEntityToDTO(MemberEntity memberEntity) {
        if (memberEntity == null) return null;

        return MemberDTO.builder()
                .churchNumber(memberEntity.getChurchNumber())
                .deacon(memberEntity.isDeacon())
                .title(memberEntity.getTitle())
                .firstName(memberEntity.getFirstName())
                .fatherName(memberEntity.getFatherName())
                .grandFatherName(memberEntity.getGrandFatherName())
                .motherName(memberEntity.getMotherName())
                .mothersFather(memberEntity.getMothersFather())
                .firstNameT(memberEntity.getFirstNameT())
                .fatherNameT(memberEntity.getFatherNameT())
                .grandFatherNameT(memberEntity.getGrandFatherNameT())
                .motherFullNameT(memberEntity.getMotherFullNameT())
                .gender(memberEntity.getGender())
                .birthday(memberEntity.getBirthday())
                .nationality(memberEntity.getNationality())
                .placeOfBirth(memberEntity.getPlaceOfBirth())
                .email(memberEntity.getEmail())
                .phone(memberEntity.getPhone())
                .whatsApp(memberEntity.getWhatsApp())
                .emergencyContactNumber(memberEntity.getEmergencyContactNumber())
                .contactRelation(memberEntity.getContactRelation())
                .eritreaContact(memberEntity.getEritreaContact())
                .maritalStatus(memberEntity.getMaritalStatus())
                .numberOfChildren(memberEntity.getNumberOfChildren())
                .firstLanguage(memberEntity.getFirstLanguage())
                .secondLanguage(memberEntity.getSecondLanguage())
                .profession(memberEntity.getProfession())
                .levelOfEducation(memberEntity.getLevelOfEducation())
                .fatherOfConfession(memberEntity.getFatherOfConfession())
                .spouseIdNumber(memberEntity.getSpouseIdNumber())
                .address(memberEntity.getAddress())
                .build();
    }

    public MemberEntity memberDTOToEntity(MemberDTO dto) {
        if (dto == null) return null;

        return MemberEntity.builder()
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
                .eritreaContact(dto.getEritreaContact())
                .maritalStatus(dto.getMaritalStatus())
                .numberOfChildren(dto.getNumberOfChildren())
                .firstLanguage(dto.getFirstLanguage())
                .secondLanguage(dto.getSecondLanguage())
                .profession(dto.getProfession())
                .levelOfEducation(dto.getLevelOfEducation())
                .fatherOfConfession(dto.getFatherOfConfession())
                .spouseIdNumber(dto.getSpouseIdNumber())
                .address(dto.getAddress())
                // .status(null) // optionally set default value
                .build();
    }
}
