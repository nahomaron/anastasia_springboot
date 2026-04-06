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
    @Story("Platform admin manages an existing church")
    void platformAdminCanManageExistingChurch() {
        RequestSpecification adminSpec = getSpecForRole("PLATFORM_ADMIN");

        Response listResponse = churchService.listChurches(adminSpec);
        assertThat(listResponse.statusCode()).isEqualTo(200);

        Long churchId = listResponse.jsonPath().getLong("content[0].churchId");
        assertThat(churchId)
                .as("Church id present in listing response")
                .isNotNull();

        ChurchDTO updatePayload = ChurchDataFactory.newValidChurch();
        updatePayload.setChurchNameLocal(updatePayload.getChurchNameLocal() + " Updated");

        Response updateResponse = churchService.updateChurch(adminSpec, churchId, updatePayload);
        assertThat(updateResponse.statusCode()).isIn(200, 202);

        Response getResponse = churchService.getChurch(adminSpec, churchId);
        assertThat(getResponse.statusCode()).isEqualTo(200);
        SchemaValidator.validate(getResponse, "schemas/church-register-schema.json");
        assertThat(getResponse.jsonPath().getString("churchNameLocal"))
                .isEqualTo(updatePayload.getChurchNameLocal());
    }
}
