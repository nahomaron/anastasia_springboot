package com.anastasia.Anastasia_BackEnd.Api.tests.group;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.GroupDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ChurchService;
import com.anastasia.Anastasia_BackEnd.Api.services.GroupService;
import com.anastasia.Anastasia_BackEnd.modules.groups.dto.GroupDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Group Management")
@Feature("Happy flows")
@Severity(SeverityLevel.CRITICAL)
class GroupPositiveTests extends BaseApiTest {

    private final GroupService groupService = new GroupService();
    private final ChurchService churchService = new ChurchService();

    @Test
    @Story("Owner creates group successfully for an existing church")
    void ownerCanCreateGroup() {
        RequestSpecification ownerSpec = getSpecForRole("OWNER");

        Response registerResponse = churchService.getCurrentTenantChurch(ownerSpec);
        assertThat(registerResponse.statusCode()).isEqualTo(200);

        String churchId = registerResponse.jsonPath().getString("id");
        assertThat(churchId)
                .as("Church id available for group creation")
                .isNotBlank();

        GroupDTO groupPayload = GroupDataFactory.newGroup(churchId);
        Response createGroup = groupService.createGroup(ownerSpec, groupPayload);
        assertThat(createGroup.statusCode()).isIn(200, 201);
    }
}
