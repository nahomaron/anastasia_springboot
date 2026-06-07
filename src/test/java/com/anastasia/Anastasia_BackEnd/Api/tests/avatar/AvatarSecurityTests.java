package com.anastasia.Anastasia_BackEnd.Api.tests.avatar;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.AvatarDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ImageAssetService;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Avatar Management")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class AvatarSecurityTests extends BaseApiTest {

    @Test
    void savingAvatarWithoutTokenShouldReturnUnauthorized() {
        UUID userId = BaseApiTest.getCachedUserId();

        var response = given()
                .contentType(ContentType.JSON)
                .body(AvatarDataFactory.newValidAvatar())
                .when()
                .post("/images/USER/" + userId)
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(401);
    }
}
