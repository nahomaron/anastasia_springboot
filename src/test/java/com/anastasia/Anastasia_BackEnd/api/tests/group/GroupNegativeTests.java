package com.anastasia.Anastasia_BackEnd.api.tests.group;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.GroupDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.GroupService;
import com.anastasia.Anastasia_BackEnd.model.group.AddUsersToGroupRequest;
import com.anastasia.Anastasia_BackEnd.model.group.GroupDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Group Management")
@Feature("Negative coverage")
@Severity(SeverityLevel.NORMAL)
class GroupNegativeTests extends BaseApiTest {

    private final GroupService groupService = new GroupService();

    @Test
    @Story("Validation error when creating without name")
    void creatingGroupWithoutNameShouldFail() {
        GroupDTO payload = GroupDataFactory.newGroup(null);
        payload.setGroupName(null);

        Response response = groupService.createGroup(getSpecForRole("OWNER"), payload);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Story("Adding users to unknown group returns error")
    void addingUsersToUnknownGroupShouldFail() {
        AddUsersToGroupRequest request = GroupDataFactory.addUsersRequest(Set.of(UUID.randomUUID()));
        Response response = groupService.addUsers(getSpecForRole("OWNER"), 9_999_999L, request);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
