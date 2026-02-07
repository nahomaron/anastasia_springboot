package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public Adult_MemberDTO memberEntityToDTO(Adult_MemberEntity adultMemberEntity) {
        if (adultMemberEntity == null) return null;

        return Adult_MemberDTO.builder()
                .churchNumber(adultMemberEntity.getChurchNumber())
                .deacon(adultMemberEntity.isDeacon())
                .title(adultMemberEntity.getTitle())
                .firstName(adultMemberEntity.getFirstName())
                .fatherName(adultMemberEntity.getFatherName())
                .grandFatherName(adultMemberEntity.getGrandFatherName())
                .motherName(adultMemberEntity.getMotherName())
                .mothersFather(adultMemberEntity.getMothersFather())
                .firstNameT(adultMemberEntity.getFirstNameT())
                .fatherNameT(adultMemberEntity.getFatherNameT())
                .grandFatherNameT(adultMemberEntity.getGrandFatherNameT())
                .motherFullNameT(adultMemberEntity.getMotherFullNameT())
                .gender(adultMemberEntity.getGender())
                .birthday(adultMemberEntity.getBirthday())
                .nationality(adultMemberEntity.getNationality())
                .placeOfBirth(adultMemberEntity.getPlaceOfBirth())
                .email(adultMemberEntity.getEmail())
                .phone(adultMemberEntity.getPhone())
                .whatsApp(adultMemberEntity.getWhatsApp())
                .emergencyContactNumber(adultMemberEntity.getEmergencyContactNumber())
                .contactRelation(adultMemberEntity.getContactRelation())
                .eritreaContact(adultMemberEntity.getEritreaContact())
                .maritalStatus(adultMemberEntity.getMaritalStatus())
                .numberOfChildren(adultMemberEntity.getNumberOfChildren())
                .firstLanguage(adultMemberEntity.getFirstLanguage())
                .secondLanguage(adultMemberEntity.getSecondLanguage())
                .profession(adultMemberEntity.getProfession())
                .levelOfEducation(adultMemberEntity.getLevelOfEducation())
                .fatherOfConfession(adultMemberEntity.getFatherOfConfession())
                .spouseIdNumber(adultMemberEntity.getSpouseIdNumber())
                .address(adultMemberEntity.getAddress())
                .termsAccepted(adultMemberEntity.isTermsAccepted())
                .termsVersion(adultMemberEntity.getTermsVersion())
                .termsAcceptedAt(adultMemberEntity.getTermsAcceptedAt())
                .build();
    }

    public Adult_MemberEntity memberDTOToEntity(Adult_MemberDTO dto) {
        if (dto == null) return null;

        return Adult_MemberEntity.builder()
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
                .termsAccepted(dto.isTermsAccepted())
                .termsVersion(dto.getTermsVersion())
                .termsAcceptedAt(dto.getTermsAcceptedAt())
                // .status(null) // optionally set default value
                .build();
    }
}
