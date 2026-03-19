package com.anastasia.Anastasia_BackEnd.modules.registration.mappers;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult.Adult_MemberEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetEntity;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.EducationLevel;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.MemberGender;
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
                .village(childMemberEntity.getVillage())
                .email(childMemberEntity.getEmail())
                .phone(childMemberEntity.getPhone())
                .whatsApp(childMemberEntity.getWhatsApp())
                .emergencyContactNumber(childMemberEntity.getEmergencyContactNumber())
                .contactRelation(childMemberEntity.getContactRelation())
                .primaryGuardianPhone(childMemberEntity.getPrimaryGuardianPhone())
                .guardianRelationship(childMemberEntity.getGuardianRelationship())
                .firstLanguage(childMemberEntity.getFirstLanguage())
                .secondLanguage(childMemberEntity.getSecondLanguage())
                .levelOfEducation(childMemberEntity.getLevelOfEducation())
                .fatherOfConfession(childMemberEntity.getFatherOfConfession())
                .churchOfBaptism(childMemberEntity.getChurchOfBaptism())
                .baptismName(childMemberEntity.getBaptismName())
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
                .village(childMemberEntity.getVillage())
                .email(childMemberEntity.getEmail())
                .phone(childMemberEntity.getPhone())
                .whatsApp(childMemberEntity.getWhatsApp())
                .emergencyContactNumber(childMemberEntity.getEmergencyContactNumber())
                .contactRelation(childMemberEntity.getContactRelation())
                .approvedByChurch(childMemberEntity.isApprovedByChurch())
                .primaryGuardianPhone(childMemberEntity.getPrimaryGuardianPhone())
                .guardianRelationship(childMemberEntity.getGuardianRelationship())
                .firstLanguage(childMemberEntity.getFirstLanguage())
                .secondLanguage(childMemberEntity.getSecondLanguage())
                .levelOfEducation(childMemberEntity.getLevelOfEducation())
                .fatherOfConfession(childMemberEntity.getFatherOfConfession())
                .churchOfBaptism(childMemberEntity.getChurchOfBaptism())
                .baptismName(childMemberEntity.getBaptismName())
                .priestNumber(childMemberEntity.getPriestNumber())
                .address(childMemberEntity.getAddress())
                .userId(childMemberEntity.getUserId())
                .churchId(childMemberEntity.getChurchId())
                .createdAt(childMemberEntity.getCreatedAt())
                .updatedAt(childMemberEntity.getUpdatedAt())
                .registeredAt(childMemberEntity.getRegisteredAt())
                .approvedAt(childMemberEntity.getApprovedAt())
                .father(buildParentSummary(childMemberEntity.getFather()))
                .mother(buildParentSummary(childMemberEntity.getMother()))
                .build();
    }

    public Child_MemberSummaryResponse childEntityToSummaryResponse(Child_MemberEntity childMemberEntity, String language) {
        if (childMemberEntity == null) return null;

        String fullName = joinNameParts(
                childMemberEntity.getFirstName(),
                childMemberEntity.getFatherName(),
                childMemberEntity.getGrandFatherName()
        );
        String fullNameLocal = joinNameParts(
                childMemberEntity.getFirstNameT(),
                childMemberEntity.getFatherNameT(),
                childMemberEntity.getGrandFatherNameT()
        );

        return Child_MemberSummaryResponse.builder()
                .id(childMemberEntity.getId())
                .membershipNumber(childMemberEntity.getMembershipNumber())
                .status(childMemberEntity.getStatus())
                .avatarUrl(resolveAvatarUrl(childMemberEntity.getAvatar()))
                .fullName(fullName)
                .fullNameLocal(fullNameLocal)
                .displayName(resolveDisplayName(language, fullName, fullNameLocal))
                .firstName(childMemberEntity.getFirstName())
                .fatherName(childMemberEntity.getFatherName())
                .grandFatherName(childMemberEntity.getGrandFatherName())
                .firstNameT(childMemberEntity.getFirstNameT())
                .fatherNameT(childMemberEntity.getFatherNameT())
                .grandFatherNameT(childMemberEntity.getGrandFatherNameT())
                .email(childMemberEntity.getEmail())
                .phone(childMemberEntity.getPhone())
                .createdAt(childMemberEntity.getCreatedAt())
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
                .genderValue(MemberGender.from(dto.getGender()))
                .birthday(dto.getBirthday())
                .nationality(dto.getNationality())
                .placeOfBirth(dto.getPlaceOfBirth())
                .village(dto.getVillage())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .whatsApp(dto.getWhatsApp())
                .emergencyContactNumber(dto.getEmergencyContactNumber())
                .contactRelation(dto.getContactRelation())
                .primaryGuardianPhone(dto.getPrimaryGuardianPhone())
                .guardianRelationship(dto.getGuardianRelationship())
                .firstLanguage(dto.getFirstLanguage())
                .secondLanguage(dto.getSecondLanguage())
                .educationLevelValue(EducationLevel.from(dto.getLevelOfEducation()))
                .fatherOfConfession(dto.getFatherOfConfession())
                .churchOfBaptism(dto.getChurchOfBaptism())
                .baptismName(dto.getBaptismName())
                .priestNumber(dto.getPriestNumber())
                .address(dto.getAddress())
                .build();
    }

    private ImageAssetDTO mapAvatar(ImageAssetEntity avatar) {
        if (avatar == null) {
            return null;
        }
        return ImageAssetDTO.builder()
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

    private String joinNameParts(String... parts) {
        return String.join(" ", parts == null ? new String[0] : parts)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String resolveDisplayName(String language, String fullName, String fullNameLocal) {
        if ("ti".equalsIgnoreCase(language) && fullNameLocal != null && !fullNameLocal.isBlank()) {
            return fullNameLocal;
        }
        return fullName != null && !fullName.isBlank() ? fullName : fullNameLocal;
    }

    private String resolveAvatarUrl(ImageAssetEntity avatar) {
        return avatar != null ? avatar.getImageUrl() : null;
    }
}
