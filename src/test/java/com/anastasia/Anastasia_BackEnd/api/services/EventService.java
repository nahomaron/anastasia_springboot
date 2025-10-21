package com.anastasia.Anastasia_BackEnd.api.services;

import com.anastasia.Anastasia_BackEnd.api.config.RequestSpecFactory;
import com.anastasia.Anastasia_BackEnd.model.event.EventDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.CheckInQRRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.CheckInRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.attendance.MarkAbsentRequestDTO;
import com.anastasia.Anastasia_BackEnd.model.event.requests.AssignEventManagerRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * API client facade around event endpoints.
 */
public class EventService {

    private static final String BASE_PATH = "/events";

    @Step("Create event")
    public Response createEvent(RequestSpecification spec, EventDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH)
                .then()
                .extract()
                .response();
    }

    public Response createEvent(EventDTO payload) {
        return createEvent(RequestSpecFactory.authenticatedSpec(), payload);
    }

    public Response updateEvent(RequestSpecification spec, Long eventId, EventDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .put(BASE_PATH + "/" + eventId + "/update")
                .then()
                .extract()
                .response();
    }

    public Response assignManager(RequestSpecification spec, Long eventId, AssignEventManagerRequest payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/" + eventId + "/managers")
                .then()
                .extract()
                .response();
    }

    public Response removeManager(RequestSpecification spec, Long eventId, UUID managerId) {
        return given()
                .spec(spec)
                .when()
                .delete(BASE_PATH + "/" + eventId + "/managers/" + managerId)
                .then()
                .extract()
                .response();
    }

    public Response listManagers(RequestSpecification spec, Long eventId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/" + eventId + "/managers")
                .then()
                .extract()
                .response();
    }

    public Response deleteEvent(RequestSpecification spec, Long eventId) {
        return given()
                .spec(spec)
                .when()
                .delete(BASE_PATH + "/" + eventId)
                .then()
                .extract()
                .response();
    }

    public Response checkIn(RequestSpecification spec, CheckInRequestDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/event/check-in")
                .then()
                .extract()
                .response();
    }

    public Response checkInWithQr(RequestSpecification spec, CheckInQRRequestDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/event/check-in/qr-code")
                .then()
                .extract()
                .response();
    }

    public Response markAbsent(RequestSpecification spec, MarkAbsentRequestDTO payload) {
        return given()
                .spec(spec)
                .body(payload)
                .when()
                .post(BASE_PATH + "/mark-absent")
                .then()
                .extract()
                .response();
    }

    public Response attendanceByEvent(RequestSpecification spec, Long eventId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/by-event/" + eventId)
                .then()
                .extract()
                .response();
    }

    public Response attendanceByEventAndStatus(RequestSpecification spec, Long eventId, String status) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/by-event/" + eventId + "/status/" + status)
                .then()
                .extract()
                .response();
    }

    public Response attendanceByUser(RequestSpecification spec, UUID userId) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/by-user/" + userId)
                .then()
                .extract()
                .response();
    }

    public Response attendanceByUserAndStatus(RequestSpecification spec, UUID userId, String status) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/by-user/" + userId + "/status/" + status)
                .then()
                .extract()
                .response();
    }

    public Response listVisibleEvents(RequestSpecification spec) {
        return given()
                .spec(spec)
                .when()
                .get(BASE_PATH + "/visible")
                .then()
                .extract()
                .response();
    }

    public Response listVisibleEvents() {
        return listVisibleEvents(RequestSpecFactory.authenticatedSpec());
    }
}
