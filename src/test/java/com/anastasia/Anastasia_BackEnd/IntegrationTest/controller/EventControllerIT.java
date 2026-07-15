package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Epic("Integration Tests")
@Feature("Internal Layer")
@SpringBootTest
@ActiveProfiles("test")
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Transactional
public class EventControllerIT extends PostgresTestContainer {
    // This class is used for integration tests of the EventController.
    // It will contain test methods that interact with the EventController
    // and verify its behavior in a real application context.

    @Autowired private MockMvc mockMvc;

    @Test
    void updateEntryShouldRejectUsersWithoutWritePermission() throws Exception {
        mockMvc.perform(put("/api/v1/calendar/entries/00000000-0000-0000-0000-000000000001")
                        .with(csrf())
                        .with(user("calendar-reader").authorities(() -> "VIEW_EVENTS"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "EVENT",
                                  "title": "Updated title",
                                  "description": "Updated description",
                                  "calendarSystem": "GREGORIAN",
                                  "startAtUtc": "2026-01-01T10:00:00Z",
                                  "endAtUtc": "2026-01-01T11:00:00Z",
                                  "timezone": "UTC",
                                  "allDay": false,
                                  "visibility": "PUBLIC",
                                  "categories": ["EVENTS"],
                                  "recurrence": { "frequency": "NONE" },
                                  "audienceUserIds": [],
                                  "audienceGroupIds": []
                                }
                                """))
                .andExpect(status().isForbidden());
    }

}
