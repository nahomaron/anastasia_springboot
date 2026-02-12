package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.child;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMemberResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Child_MemberResponse extends BaseMemberResponse {

    private Long id;
    private ParentSummary father;
    private ParentSummary mother;
}
