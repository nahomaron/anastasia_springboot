package com.anastasia.Anastasia_BackEnd.model.DTO.membership;

import com.anastasia.Anastasia_BackEnd.model.DTO.embeded.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Child {
    private UUID userId;
    private UUID tenantId;
    private Long churchId;
    private UUID parentId;
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
    private String placeOfBirth;
    private Address address;
    private String email;
    private String phone;
    private String whatsApp;
    private String emergencyContactNumber;
    private String contactRelation;
    private String firstLanguage;
    private String secondLanguage;
    private String levelOfEducation;
    private String fatherOfConfession;
}
