package com.anastasia.Anastasia_BackEnd.api.factories;

import com.anastasia.Anastasia_BackEnd.model.group.AddUsersToGroupRequest;
import com.anastasia.Anastasia_BackEnd.model.group.BatchInviteRequest;
import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import com.anastasia.Anastasia_BackEnd.model.group.RemoveUsersFromGroupRequest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Factory for generating payloads in the Group domain.
 */
public final class GroupDataFactory {

    private GroupDataFactory() {
    }

    public static GroupDTO newGroup(String churchId) {
        return GroupDTO.builder()
                .churchId(churchId)
                .groupName("Youth Choir " + System.currentTimeMillis())
                .description("Music ministry serving the Sunday liturgy.")
                .avatar("https://cdn.example.com/groups/choir.png")
                .visibility("PUBLIC")
                .build();
    }

    public static GroupDTO updateGroupPayload(String churchId) {
        GroupDTO dto = newGroup(churchId);
        dto.setDescription("Updated description for ministry group.");
        dto.setVisibility("PRIVATE");
        return dto;
    }

    public static AddUsersToGroupRequest addUsersRequest(Set<UUID> users) {
        return AddUsersToGroupRequest.builder()
                .userIds(users)
                .build();
    }

    public static RemoveUsersFromGroupRequest removeUsersRequest(List<UUID> users) {
        return RemoveUsersFromGroupRequest.builder()
                .userIds(users)
                .build();
    }

    public static BatchInviteRequest inviteRequest(Set<String> emails) {
        return BatchInviteRequest.builder()
                .groupName("Volunteers")
                .groupDescription("Service volunteers invitation")
                .groupType("MINISTRY")
                .groupImageUrl("https://cdn.example.com/groups/volunteers.png")
                .groupBannerUrl("https://cdn.example.com/groups/volunteers-banner.png")
                .groupColor("#3366FF")
                .groupIcon("mdi-account-group")
                .groupLocation("Community Center")
                .groupWebsite("https://volunteers.example.com")
                .groupEmails(emails)
                .groupPhoneNumber("+12025551111")
                .groupAddress("501 Volunteer Way, Alexandria, VA")
                .build();
    }
}
