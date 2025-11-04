package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentSummary {

    private Long id;
    private String membershipNumber;
    private String fullName;
}

