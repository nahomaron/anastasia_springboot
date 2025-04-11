package com.anastasia.Anastasia_BackEnd.model.member;

import com.anastasia.Anastasia_BackEnd.model.common.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDTO {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{1,2}\\d{5}$", message = "Invalid church number")
    private String churchNumber;

    private boolean deacon;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Father's name is required")
    private String fatherName;

    @NotBlank(message = "Grandfather's name is required")
    private String grandFatherName;

    @NotBlank(message = "Mother's name is required")
    private String motherName;

    @NotBlank(message = "Mother's father name is required")
    private String mothersFather;

    @NotBlank(message = "First name (in Tigrinya) is required")
    private String firstNameT;

    @NotBlank(message = "Father's name (in Tigrinya) is required")
    private String fatherNameT;

    @NotBlank(message = "Grandfather's name (in Tigrinya) is required")
    private String grandFatherNameT;

    @NotBlank(message = "Mother's full name (in Tigrinya) is required")
    private String motherFullNameT;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female)$", message = "Gender is required'")
    private String gender;

    @NotNull(message = "Birthday is required")
    private LocalDate birthday;

    @NotBlank(message = "Nationality is required")
    private String nationality;

    @NotBlank(message = "Place of birth is required")
    private String placeOfBirth;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number format")
    private String phone;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid WhatsApp number format")
    private String whatsApp;

   // @NotBlank(message = "Emergency contact number is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid emergency contact number format")
    private String emergencyContactNumber;

    @NotBlank(message = "Emergency contact relation is required")
    private String contactRelation;

    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid Eritrea contact number format")
    private String eritreaContact;

    @NotBlank(message = "Marital status is required")
    @Pattern(regexp = "^(Single|Married|Divorced|Widowed)$", message = "Invalid marital status")
    private String maritalStatus;

    @Max(value = 14, message = "Invalid number of children")
    private int numberOfChildren;

    @NotBlank(message = "First language is required")
    private String firstLanguage;

    private String secondLanguage;

    @NotBlank(message = "Profession is required")
    private String profession;

    private String levelOfEducation;

    @NotBlank(message = "Father of Confession is required")
    private String fatherOfConfession;


    private String spouseIdNumber;

    @Valid
    private Address address;

}
