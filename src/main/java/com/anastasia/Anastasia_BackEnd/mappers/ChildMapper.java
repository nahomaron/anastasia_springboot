package com.anastasia.Anastasia_BackEnd.mappers;

import com.anastasia.Anastasia_BackEnd.model.child.ChildDTO;
import com.anastasia.Anastasia_BackEnd.model.child.ChildEntity;
import org.springframework.stereotype.Component;

@Component
public class ChildMapper {

    public ChildDTO childEntityToDTO(ChildEntity childEntity) {
        if (childEntity == null) return null;

        return ChildDTO.builder()
                .churchNumber(childEntity.getChurchNumber())
                .deacon(childEntity.isDeacon())
                .title(childEntity.getTitle())
                .firstName(childEntity.getFirstName())
                .fatherName(childEntity.getFatherName())
                .grandFatherName(childEntity.getGrandFatherName())
                .motherName(childEntity.getMotherName())
                .mothersFather(childEntity.getMothersFather())
                .firstNameT(childEntity.getFirstNameT())
                .fatherNameT(childEntity.getFatherNameT())
                .grandFatherNameT(childEntity.getGrandFatherNameT())
                .motherFullNameT(childEntity.getMotherFullNameT())
                .gender(childEntity.getGender())
                .birthday(childEntity.getBirthday())
                .nationality(childEntity.getNationality())
                .placeOfBirth(childEntity.getPlaceOfBirth())
                .email(childEntity.getEmail())
                .phone(childEntity.getPhone())
                .whatsApp(childEntity.getWhatsApp())
                .emergencyContactNumber(childEntity.getEmergencyContactNumber())
                .contactRelation(childEntity.getContactRelation())
                .firstLanguage(childEntity.getFirstLanguage())
                .secondLanguage(childEntity.getSecondLanguage())
                .levelOfEducation(childEntity.getLevelOfEducation())
                .fatherOfConfession(childEntity.getFatherOfConfession())
                .address(childEntity.getAddress())
                .build();
    }

    public ChildEntity childDTOToEntity(ChildDTO dto) {
        if (dto == null) return null;

        return ChildEntity.builder()
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
