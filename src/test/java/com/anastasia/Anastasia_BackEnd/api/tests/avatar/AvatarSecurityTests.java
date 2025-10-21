package com.anastasia.Anastasia_BackEnd.api.tests.avatar;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.AvatarDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.AvatarService;
import com.anastasia.Anastasia_BackEnd.api.utils.UserLookupHelper;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Avatar Management")
@Feature("Security")
@Severity(SeverityLevel.CRITICAL)
@Owner("API Guild")
class AvatarSecurityTests extends BaseApiTest {

    private final AvatarService avatarService = new AvatarService();

    @Test
    void savingAvatarWithoutTokenShouldReturnForbidden() {
        UUID userId = BaseApiTest.getCachedUserId();

        var response = given()
                .contentType(ContentType.JSON)
                .body(AvatarDataFactory.newValidAvatar())
                .when()
                .post("/avatars/USER/" + userId)
                .then()
                .extract()
                .response();

        assertThat(response.statusCode()).isEqualTo(403);
    }
}
