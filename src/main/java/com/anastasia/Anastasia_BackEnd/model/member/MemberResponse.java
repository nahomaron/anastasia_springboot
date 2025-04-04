package com.anastasia.Anastasia_BackEnd.model.member;

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
