package com.anastasia.Anastasia_BackEnd.api.tests.church;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.ChurchDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.ChurchService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Church Management")
@Feature("Negative coverage")
@Severity(SeverityLevel.NORMAL)
class ChurchNegativeTests extends BaseApiTest {

    private final ChurchService churchService = new ChurchService();

    @Test
    @Story("Validation errors bubble up for missing data")
    void registeringChurchWithoutNameShouldFail() {
        Response response = churchService.registerChurch(
                getSpecForRole("OWNER"),
                ChurchDataFactory.missingName());

        assertThat(response.statusCode()).isBetween(400, 499);
    }

    @Test
    @Story("Getting unknown church returns 404")
    void unknownChurchShouldReturnNotFound() {
        Response response = churchService.getChurch(getSpecForRole("PLATFORM_ADMIN"), 9_999_999L);
        assertThat(response.statusCode()).isEqualTo(404);
    }
}
