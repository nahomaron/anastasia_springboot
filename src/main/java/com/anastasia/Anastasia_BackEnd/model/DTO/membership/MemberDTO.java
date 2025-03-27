package com.anastasia.Anastasia_BackEnd.model.DTO.membership;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDTO {

    private UUID userId;
    private UUID tenantId;
    private Long churchId;
    private String status;
    private String idCardNumber;
    private boolean deacon;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String motherName;
    private String mothersFather;
    private String firstNameT;
    private String fatherNameT;
    private String grandFatherNameT;
    private String motherFullNameT;
    private String gender;
    private String birthday;
    private String nationality;
    private String place_of_birth;
    private String address_country;
    private String current_address;
    private String province;
    private String city;
    private String email;
    private String country_phone_code;
    private String phone;
    private String whatsApp;
    private String emergencyContactNumber;
    private String contactRelation;
    private String eritreaContact;
    private String postalCode;
    private String maritalStatus;
    private String children;
    private String firstLanguage;
    private String secondLanguage;
    private String profession;
    private String levelOfEducation;
    private String fatherOfConfession;
    private String spouseId;
}
