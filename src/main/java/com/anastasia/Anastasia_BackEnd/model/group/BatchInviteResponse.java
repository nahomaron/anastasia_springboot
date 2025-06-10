package com.anastasia.Anastasia_BackEnd.model.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchInviteResponse{
    private String groupName;
//    private String groupDescription;
//    private String groupType;
//    private String groupImageUrl;
//    private String groupBannerUrl;
//    private String groupColor;
//    private String groupIcon;
//    private String groupLocation;
//    private String groupWebsite;
//    private String groupPhoneNumber;
//    private String groupAddress;
//    private int totalInvitesSent;
//    private int totalInvitesFailed;
    private int invitedCount;
}
