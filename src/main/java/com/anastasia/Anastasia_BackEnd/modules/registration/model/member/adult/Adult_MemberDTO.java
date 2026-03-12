package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Adult_MemberDTO {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{1,2}\\d{5}$", message = "{validation.member.churchNumber.invalid}")
    private String churchNumber;

    @Valid
    @NotNull
    private ImageAssetDTO avatar;

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

    @NotBlank(message = "{validation.member.phone.required}")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "{validation.member.phone.invalid}")
    private String phone;

    @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "{validation.member.whatsApp.invalid}")
    private String whatsApp;

   // @NotBlank(message = "Emergency contact number is required")
    @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "{validation.member.emergencyContact.invalid}")
    private String emergencyContactNumber;

    private String contactRelation;

    @Pattern(regexp = "^$|^\\+?[0-9]{7,15}$", message = "{validation.member.eritreaContact.invalid}")
    private String eritreaContact;

    @NotBlank(message = "{validation.member.maritalStatus.required}")
    @Pattern(regexp = "^(Single|Married|Divorced|Widowed)$", message = "{validation.member.maritalStatus.invalid}")
    private String maritalStatus;

    @Max(value = 14, message = "{validation.member.numberOfChildren.invalid}")
    private int numberOfChildren;

    @NotBlank(message = "{validation.member.firstLanguage.required}")
    private String firstLanguage;

    private String secondLanguage;

    @NotBlank(message = "{validation.member.profession.required}")
    private String profession;

    private String levelOfEducation;

    @NotBlank(message = "{validation.member.fatherOfConfession.required}")
    private String fatherOfConfession;

    private String churchOfBaptism;

    private String baptismName;

    private String priestNumber;

    private String spouseIdNumber;

    @Valid
    private Address address;

    @JsonProperty("terms_accepted")
    @JsonAlias("termsAccepted")
    @AssertTrue(message = "{validation.member.terms.accepted}")
    private boolean termsAccepted;

    @JsonProperty("terms_version")
    @JsonAlias("termsVersion")
    @NotBlank(message = "{validation.member.terms.version.required}")
    private String termsVersion;

    @JsonProperty("terms_accepted_at")
    @JsonAlias("termsAcceptedAt")
    @NotNull(message = "{validation.member.terms.acceptedAt.required}")
    private Instant termsAcceptedAt;

}
