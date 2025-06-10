package com.anastasia.Anastasia_BackEnd.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchInviteRequest {
    private String groupName;
    private String groupDescription;
    private String groupType;
    private String groupImageUrl;
    private String groupBannerUrl;
    private String groupColor;
    private String groupIcon;
    private String groupLocation;
    private String groupWebsite;
    private Set<String> groupEmails;
    private String groupPhoneNumber;
    private String groupAddress;
}
