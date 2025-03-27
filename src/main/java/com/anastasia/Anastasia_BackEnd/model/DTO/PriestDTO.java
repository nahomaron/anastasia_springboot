package com.anastasia.Anastasia_BackEnd.model.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriestDTO {

    private Long id;
    private Long churchId;
    private UUID tenantId;
    private String profilePicture;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String phoneNumber;
    private String personalEmail;
    private String churchEmail;
    private String nationalIdNumber;
    private String nationality;
    private String priesthoodCardId; // (if any)
    private String priesthoodCardScan;
    private String birthdate;
    private String prefixes; //(additional title)
    private String address;
    private String languages;
    private String levelOfEducation;
    private String roles;

}
