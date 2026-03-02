package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.avatar.AvatarEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.Child_MemberSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child.ParentSummary;
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
                .priestNumber(childMemberEntity.getPriestNumber())
                .address(childMemberEntity.getAddress())
                .father(buildParentSummary(childMemberEntity.getFather()))
                .mother(buildParentSummary(childMemberEntity.getMother()))
                .build();
    }

    public Child_MemberResponse childEntityToResponse(Child_MemberEntity childMemberEntity) {
        if (childMemberEntity == null) return null;

        return Child_MemberResponse.builder()
                .id(childMemberEntity.getId())
                .tenantId(childMemberEntity.getTenantId())
                .membershipNumber(childMemberEntity.getMembershipNumber())
                .churchNumber(childMemberEntity.getChurchNumber())
                .status(childMemberEntity.getStatus())
                .deacon(childMemberEntity.isDeacon())
                .avatar(mapAvatar(childMemberEntity.getAvatar()))
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
                .priestNumber(childMemberEntity.getPriestNumber())
                .address(childMemberEntity.getAddress())
                .userId(childMemberEntity.getUserId())
                .churchId(childMemberEntity.getChurchId())
                .createdDate(childMemberEntity.getCreatedDate())
                .lastModifiedDate(childMemberEntity.getLastModifiedDate())
                .father(buildParentSummary(childMemberEntity.getFather()))
                .mother(buildParentSummary(childMemberEntity.getMother()))
                .build();
    }

    public Child_MemberSummaryResponse childEntityToSummaryResponse(Child_MemberEntity childMemberEntity) {
        if (childMemberEntity == null) return null;

        return Child_MemberSummaryResponse.builder()
                .id(childMemberEntity.getId())
                .membershipNumber(childMemberEntity.getMembershipNumber())
                .status(childMemberEntity.getStatus())
                .firstName(childMemberEntity.getFirstName())
                .fatherName(childMemberEntity.getFatherName())
                .grandFatherName(childMemberEntity.getGrandFatherName())
                .email(childMemberEntity.getEmail())
                .phone(childMemberEntity.getPhone())
                .createdDate(childMemberEntity.getCreatedDate())
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
                .priestNumber(dto.getPriestNumber())
                .address(dto.getAddress())
                .build();
    }

    private AvatarDTO mapAvatar(AvatarEntity avatar) {
        if (avatar == null) {
            return null;
        }
        return AvatarDTO.builder()
                .imageUrl(avatar.getImageUrl())
                .imageSize(avatar.getImageSize())
                .build();
    }

    private ParentSummary buildParentSummary(Adult_MemberEntity parent) {
        if (parent == null) {
            return null;
        }

        String fullName = String.join(" ",
                parent.getFirstName() != null ? parent.getFirstName() : "",
                parent.getFatherName() != null ? parent.getFatherName() : "",
                parent.getGrandFatherName() != null ? parent.getGrandFatherName() : "").trim();

        if (fullName.isBlank()) {
            fullName = null;
        }

        return ParentSummary.builder()
                .id(parent.getId())
                .membershipNumber(parent.getMembershipNumber())
                .fullName(fullName)
                .build();
    }
}
