package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child_MemberSummaryResponse {
    private Long id;
    private String membershipNumber;
    private String status;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String email;
    private String phone;
    private Instant createdAt;
}
