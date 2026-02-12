package com.anastasia.Anastasia_BackEnd.modules.registration.model.member.adult;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.member.BaseMemberResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Adult_MemberResponse extends BaseMemberResponse {

    private Long id;
    private boolean approvedByChurch;
    private boolean approvedByPriest;
    private boolean termsAccepted;
    private String termsVersion;
    private Instant termsAcceptedAt;
    private String eritreaContact;
    private String maritalStatus;
    private int numberOfChildren;
    private String profession;
    private String spouseIdNumber;
    private Set<Long> childrenAsFatherIds;
    private Set<Long> childrenAsMotherIds;
}
