package com.anastasia.Anastasia_BackEnd.modules.registration.model.priest;

import com.anastasia.Anastasia_BackEnd.modules.registration.common.Address;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.imageasset.ImageAssetDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriestResponse {

    private Long id;

    private String priestNumber;

    private String churchNumber;

    private UUID tenantId;

    private PriestStatus status;

    private ImageAssetDTO avatar;

    private int spiritualChildren;

    private String prefixes; //(additional title)

    private String firstName;

    private String fatherName;

    private String grandFatherName;

    private String phoneNumber;

    private String personalEmail;

    private String churchEmail;

    private String priesthoodCardId; // (if any)
    private String priesthoodCardScan;

    private String birthdate;

    @Builder.Default
    private Set<String> languages = new HashSet<>();

    private String levelOfEducation;

    private Address address;

    private boolean isActive;
}
