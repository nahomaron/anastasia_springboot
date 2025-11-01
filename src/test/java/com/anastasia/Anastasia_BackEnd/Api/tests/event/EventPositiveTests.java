package com.anastasia.Anastasia_BackEnd.Api.tests.event;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.ChurchDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.factories.EventDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.ChurchService;
import com.anastasia.Anastasia_BackEnd.Api.services.EventService;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.church.ChurchDTO;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
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

    @Test
    @Story("Owner lists visible events")
    void ownerCanListVisibleEvents() {
        RequestSpecification ownerSpec = getSpecForRole("OWNER");
        Response response = eventService.listVisibleEvents(ownerSpec);
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
