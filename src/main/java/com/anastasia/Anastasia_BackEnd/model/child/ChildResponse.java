package com.anastasia.Anastasia_BackEnd.model.child;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChildResponse {

    private String membershipNumber;

    private String name;

    private String fatherOfConfession;
}
