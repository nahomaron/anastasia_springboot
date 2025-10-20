package com.anastasia.Anastasia_BackEnd.api.tests.child;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.ChildDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.ChildService;
import com.anastasia.Anastasia_BackEnd.api.utils.SchemaValidator;
import com.anastasia.Anastasia_BackEnd.model.child.ChildDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Child Registration")
@Feature("Happy flows")
@Severity(SeverityLevel.CRITICAL)
class ChildPositiveTests extends BaseApiTest {

    private final ChildService childService = new ChildService();

//    @Test
//    @Story("Priest registers child successfully")
//    void priestShouldRegisterChild() {
//        RequestSpecification priestSpec = getSpecForRole("PRIEST");
//        ChildDTO payload = ChildDataFactory.newValidChild();
//
//        Response response = childService.registerChild(priestSpec, payload);
//        assertThat(response.statusCode()).isEqualTo(201);
//        SchemaValidator.validate(response, "schemas/child-response-schema.json");
//    }

    @Test
    @Story("Priest lists children")
    void priestShouldListChildren() {
        RequestSpecification priestSpec = getSpecForRole("PRIEST");
        Response response = childService.listChildren(priestSpec, Map.of("page", 0, "size", 5));
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @Story("Advanced search returns HTTP 200")
    void advancedSearchShouldRespondWithOk() {
        RequestSpecification priestSpec = getSpecForRole("PRIEST");
        ChildDTO payload = ChildDataFactory.newValidChild();

        Response response = childService.advancedSearch(
                priestSpec,
                Map.of("status", "PENDING"),
                payload.getAddress());

        assertThat(response.statusCode()).isBetween(200, 299);
    }
}
