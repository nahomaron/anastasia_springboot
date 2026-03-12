package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child_MemberDTO {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{1,2}\\d{5}$", message = "{validation.member.churchNumber.invalid}")
    private String churchNumber;

    private boolean deacon;

    @NotBlank(message = "{validation.member.title.required}")
    private String title;

    @NotBlank(message = "{validation.member.firstName.required}")
    private String firstName;

    @NotBlank(message = "{validation.member.fatherName.required}")
    private String fatherName;

    @NotBlank(message = "{validation.member.grandFatherName.required}")
    private String grandFatherName;

    @NotBlank(message = "{validation.member.motherName.required}")
    private String motherName;

    @NotBlank(message = "{validation.member.mothersFather.required}")
    private String mothersFather;

    @NotBlank(message = "{validation.member.firstNameLocal.required}")
    private String firstNameT;

    @NotBlank(message = "{validation.member.fatherNameLocal.required}")
    private String fatherNameT;

    @NotBlank(message = "{validation.member.grandFatherNameLocal.required}")
    private String grandFatherNameT;

    @NotBlank(message = "{validation.member.motherFullNameLocal.required}")
    private String motherFullNameT;

    @NotBlank(message = "{validation.member.gender.required}")
    @Pattern(regexp = "^(Male|Female)$", message = "{validation.member.gender.invalid}")
    private String gender;

    @NotNull(message = "{validation.member.birthday.required}")
    private LocalDate birthday;

    @NotBlank(message = "{validation.member.nationality.required}")
    private String nationality;

    @NotBlank(message = "{validation.member.placeOfBirth.required}")
    private String placeOfBirth;

    private String village;

    @Email(message = "{validation.member.email.invalid}")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "{validation.member.phone.invalid}")
    private String phone;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "{validation.member.whatsApp.invalid}")
    private String whatsApp;

     @NotBlank(message = "{validation.child.emergencyContact.required}")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "{validation.member.emergencyContact.invalid}")
    private String emergencyContactNumber;

    @NotBlank(message = "{validation.child.contactRelation.required}")
    private String contactRelation;

    @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "{validation.child.guardianPhone.invalid}")
    private String primaryGuardianPhone;

    private String guardianRelationship;

    @NotBlank(message = "{validation.member.firstLanguage.required}")
    private String firstLanguage;

    private String secondLanguage;

    private String levelOfEducation;

    @NotBlank(message = "{validation.member.fatherOfConfession.required}")
    private String fatherOfConfession;

    private String churchOfBaptism;

    private String baptismName;

    private String priestNumber;

    @Valid
    private Address address;

    @Valid
    private ParentSummary father;

    @Valid
    private ParentSummary mother;

}
