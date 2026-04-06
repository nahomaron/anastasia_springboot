package com.anastasia.Anastasia_BackEnd.Api.tests.child;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.ChildDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ChildService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Child Registration")
@Feature("Negative coverage")
@Severity(SeverityLevel.NORMAL)
class ChildNegativeTests extends BaseApiTest {

    private final ChildService childService = new ChildService();

    @Test
    @Story("Validation error returned for missing fields")
    void registeringChildWithoutRequiredFieldsShouldFail() {
        Response response = childService.registerChild(
                getSpecForRole("PRIEST"),
                ChildDataFactory.missingRequiredField());

        assertThat(response.statusCode()).isBetween(400, 499);
    }

    @Test
    @Story("Retrieving non-existing child returns a client error")
    void getUnknownChildShouldReturnNotFound() {
        Response response = childService.getChild(getSpecForRole("PRIEST"), 9_999_999L);
        assertThat(response.statusCode()).isIn(400, 404);
    }
}
