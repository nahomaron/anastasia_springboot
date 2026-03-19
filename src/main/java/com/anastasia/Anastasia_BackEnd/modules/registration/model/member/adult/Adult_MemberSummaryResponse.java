package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adult_MemberSummaryResponse {
    private Long id;
    private String membershipNumber;
    private String status;
    private String fullName;
    private String fullNameLocal;
    private String displayName;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String firstNameT;
    private String fatherNameT;
    private String grandFatherNameT;
    private String email;
    private String phone;
    private Instant createdAt;
}
