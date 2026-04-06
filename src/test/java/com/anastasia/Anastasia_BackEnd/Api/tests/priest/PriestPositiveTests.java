package com.anastasia.Anastasia_BackEnd.Api.tests.priest;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.ChurchDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.PriestDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ChurchService;
import com.anastasia.Anastasia_BackEnd.Api.services.PriestService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.priest.PriestDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Priest Management")
@Feature("Happy flows")
@Severity(SeverityLevel.CRITICAL)
class PriestPositiveTests extends BaseApiTest {

    private final PriestService priestService = new PriestService();
    private final ChurchService churchService = new ChurchService();

    @Test
    @Story("Owner registers priest and lists roster")
    void ownerCanRegisterPriest() {
        RequestSpecification ownerSpec = getSpecForRole("OWNER");

        Response churchList = churchService.listChurches(getSpecForRole("PLATFORM_ADMIN"));
        assertThat(churchList.statusCode()).isEqualTo(200);

        String churchNumber = churchList.jsonPath().getString("content[0].churchNumber");
        Assumptions.assumeTrue(churchNumber != null && !churchNumber.isBlank(), "Existing church number available");

        PriestDTO priestPayload = PriestDataFactory.newValidPriest(churchNumber.trim(), null);
        Response register = priestService.registerPriest(ownerSpec, priestPayload);
        assertThat(register.statusCode()).isEqualTo(201);

        Response list = priestService.listPriests(getSpecForRole("OWNER"));
        assertThat(list.statusCode()).isEqualTo(200);
    }
}
