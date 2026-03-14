package com.anastasia.Anastasia_BackEnd.modules.registration.model.member;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BaseMemberResponse {

    private UUID tenantId;
    private String membershipNumber;
    private String churchNumber;
    private String status;
    private boolean deacon;
    private ImageAssetDTO avatar;

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
    private LocalDate birthday;
    private String nationality;
    private String placeOfBirth;
    private String village;
    private String email;
    private String phone;
    private String whatsApp;
    private String emergencyContactNumber;
    private String contactRelation;
    private String firstLanguage;
    private String secondLanguage;
    private String levelOfEducation;
    private String fatherOfConfession;
    private String churchOfBaptism;
    private String baptismName;
    private String priestNumber;
    private Address address;

    private UUID userId;
    private Long churchId;

    private Instant createdAt;
    private Instant updatedAt;
    private LocalDateTime registeredAt;
    private LocalDateTime approvedAt;
}
