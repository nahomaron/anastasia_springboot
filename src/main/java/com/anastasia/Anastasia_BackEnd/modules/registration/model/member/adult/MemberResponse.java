package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponse {

    private String membershipNumber;

    private String name;

    private String fatherOfConfession;
}
