package com.anastasia.Anastasia_BackEnd.api.tests.priest;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.ChurchDataFactory;
import com.anastasia.Anastasia_BackEnd.api.factories.PriestDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.ChurchService;
import com.anastasia.Anastasia_BackEnd.api.services.PriestService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Priest Management")
@Feature("Negative coverage")
@Severity(SeverityLevel.NORMAL)
class PriestNegativeTests extends BaseApiTest {

    private final PriestService priestService = new PriestService();
    private final ChurchService churchService = new ChurchService();

    @Test
    @Story("Passwords must match when registering priest")
    void registeringPriestWithMismatchedPasswordsShouldFail() {
        Response churchResponse = churchService.registerChurch(getSpecForRole("OWNER"), ChurchDataFactory.newValidChurch());
        String churchNumber = churchResponse.asString();
        Assumptions.assumeTrue(churchNumber != null && !churchNumber.isBlank(), "Church number available");

        Response response = priestService.registerPriest(
                getSpecForRole("OWNER"),
                PriestDataFactory.mismatchedPassword(churchNumber.trim(), null));

        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Story("Updating unknown priest returns not found")
    void updatingUnknownPriestShouldFail() {
        Response response = priestService.updatePriest(getSpecForRole("OWNER"), 9_999_999L,
                PriestDataFactory.newValidPriest(null, null));
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }
}
