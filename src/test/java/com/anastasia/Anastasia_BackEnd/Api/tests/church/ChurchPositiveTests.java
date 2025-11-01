package com.anastasia.Anastasia_BackEnd.Api.tests.church;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.ChurchDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ChurchService;
import com.anastasia.Anastasia_BackEnd.Api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Church Management")
@Feature("Happy flows")
@Severity(SeverityLevel.CRITICAL)
class ChurchPositiveTests extends BaseApiTest {

    private final ChurchService churchService = new ChurchService();

    @Test
    @Story("Owner registers church and platform admin manages it")
    void ownerCanRegisterAndPlatformAdminCanManageChurch() {
        RequestSpecification ownerSpec = getSpecForRole("OWNER");
        ChurchDTO newChurch = ChurchDataFactory.newValidChurch();

        Response registerResponse = churchService.registerChurch(ownerSpec, newChurch);
        assertThat(registerResponse.statusCode()).isEqualTo(201);
        SchemaValidator.validate(registerResponse, "schemas/church-register-schema.json");

        RequestSpecification adminSpec = getSpecForRole("PLATFORM_ADMIN");
        Response listResponse = churchService.listChurches(adminSpec);
        assertThat(listResponse.statusCode()).isEqualTo(200);

        Long churchId = null;
        try {
            churchId = listResponse.jsonPath().getLong("content[0].churchId");
        } catch (Exception ignored) {
            // handled below
        }
        assertThat(churchId)
                .as("Church id present in listing response")
                .isNotNull();

        ChurchDTO updatePayload = ChurchDataFactory.newValidChurch();
        updatePayload.setChurchName(updatePayload.getChurchName() + " Updated");

        Response updateResponse = churchService.updateChurch(adminSpec, churchId, updatePayload);
        assertThat(updateResponse.statusCode()).isIn(200, 202);

        Response deleteResponse = churchService.deleteChurch(adminSpec, churchId);
        assertThat(deleteResponse.statusCode()).isIn(200, 204);
    }
}
