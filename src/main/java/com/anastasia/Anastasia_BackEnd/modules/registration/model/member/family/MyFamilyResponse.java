package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.family;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyFamilyResponse {
    private FamilyMemberSummaryResponse self;
    private FamilyMemberSummaryResponse spouse;
    private List<FamilyMemberSummaryResponse> children;
    private List<FamilyMemberSummaryResponse> parents;
    private List<FamilyMemberSummaryResponse> inLaws;
}
