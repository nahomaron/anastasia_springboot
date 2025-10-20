package com.anastasia.Anastasia_BackEnd.api.tests.event;

import com.anastasia.Anastasia_BackEnd.api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.api.factories.ChurchDataFactory;
import com.anastasia.Anastasia_BackEnd.api.factories.EventDataFactory;
import com.anastasia.Anastasia_BackEnd.api.services.ChurchService;
import com.anastasia.Anastasia_BackEnd.api.services.EventService;
import com.anastasia.Anastasia_BackEnd.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.model.event.EventDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Event Management")
@Feature("Happy flows")
@Severity(SeverityLevel.CRITICAL)
class EventPositiveTests extends BaseApiTest {

    private final EventService eventService = new EventService();
    private final ChurchService churchService = new ChurchService();

    @Test
    @Story("Owner creates an event")
    void ownerCanCreateEvent() {
        RequestSpecification ownerSpec = getSpecForRole("OWNER");
        ChurchDTO churchPayload = ChurchDataFactory.newValidChurch();
        Response churchResponse = churchService.registerChurch(ownerSpec, churchPayload);
        assertThat(churchResponse.statusCode()).isEqualTo(201);

        Response listResponse = churchService.listChurches(getSpecForRole("PLATFORM_ADMIN"));
        assertThat(listResponse.statusCode()).isEqualTo(200);

        System.out.println(listResponse.asString());

        Long churchId = null;
        try {
            churchId = listResponse.jsonPath().getLong("content[0].churchId");
        } catch (Exception ignored) {
            // handled below
        }
        assertThat(churchId)
                .as("Church id available for event creation")
                .isNotNull();

        EventDTO eventPayload = EventDataFactory.newEvent(churchId);
        Response createResponse = eventService.createEvent(ownerSpec, eventPayload);
        assertThat(createResponse.statusCode()).isIn(200, 201);
    }
}
