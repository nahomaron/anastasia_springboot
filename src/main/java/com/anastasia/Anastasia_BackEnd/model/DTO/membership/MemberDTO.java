package com.anastasia.Anastasia_BackEnd.model.DTO.membership;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberDTO {

//    @JsonProperty("first_name")
    private String firstName;

//    @JsonProperty("father_name")
    private String fatherName;

//    @JsonProperty("grand_father_name")
    private String grandFatherName;
}
