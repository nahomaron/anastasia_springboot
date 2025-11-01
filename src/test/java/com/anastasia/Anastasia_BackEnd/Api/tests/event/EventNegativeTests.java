package com.anastasia.Anastasia_BackEnd.Api.tests.event;

import com.anastasia.Anastasia_BackEnd.Api.base.BaseApiTest;
import com.anastasia.Anastasia_BackEnd.Api.factories.EventDataFactory;
import com.anastasia.Anastasia_BackEnd.Api.services.EventService;
import com.anastasia.Anastasia_BackEnd.modules.events.model.EventDTO;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Event Management")
@Feature("Negative coverage")
@Severity(SeverityLevel.NORMAL)
class EventNegativeTests extends BaseApiTest {

    private final EventService eventService = new EventService();

    @Test
    @Story("Validation error when creating event with missing title")
    void creatingEventWithoutTitleShouldFail() {
        EventDTO payload = EventDataFactory.newEvent(null);
        payload.setTitle(null);

        Response response = eventService.createEvent(getSpecForRole("OWNER"), payload);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
    }

    @Test
    @Story("Updating non-existing event returns 404")
    void updatingUnknownEventShouldFail() {
        EventDTO payload = EventDataFactory.newEvent(null);
        Response response = eventService.updateEvent(getSpecForRole("OWNER"), 9_999_999L, payload);
        assertThat(response.statusCode()).isIn(404, 400);
    }

    @Test
    @Story("User without permissions cannot fetch visible events")
    void userRoleCannotListVisibleEvents() {
        Response response = eventService.listVisibleEvents(getSpecForRole("USER"));
        assertThat(response.statusCode()).isEqualTo(403);
    }
}
