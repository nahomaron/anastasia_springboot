package com.anastasia.Anastasia_BackEnd.Api.tests.group;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.ChurchDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.GroupDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ChurchService;
import com.anastasia.Anastasia_BackEnd.Api.services.GroupService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
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
    @Story("Owner creates group successfully")
    void ownerCanCreateGroup() {
        RequestSpecification ownerSpec = getSpecForRole("OWNER");

        ChurchDTO churchPayload = ChurchDataFactory.newValidChurch();
        Response registerChurch = churchService.registerChurch(ownerSpec, churchPayload);
        assertThat(registerChurch.statusCode()).isEqualTo(201);

        Response churches = churchService.listChurches(getSpecForRole("PLATFORM_ADMIN"));
        assertThat(churches.statusCode()).isEqualTo(200);

        String churchId = null;
        try {
            churchId = churches.jsonPath().getString("content[0].churchId");
        } catch (Exception ignored) {
            // handled below
        }
        assertThat(churchId)
                .as("Church id available for group creation")
                .isNotBlank();

        GroupDTO groupPayload = GroupDataFactory.newGroup(churchId);
        Response createGroup = groupService.createGroup(ownerSpec, groupPayload);
        assertThat(createGroup.statusCode()).isIn(200, 201);
    }
}
