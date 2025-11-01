package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import jakarta.transaction.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@Epic("Integration Tests")
@Feature("Internal Layer")
@SpringBootTest
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@Transactional
public class EventControllerIT extends PostgresTestContainer {
    // This class is used for integration tests of the EventController.
    // It will contain test methods that interact with the EventController
    // and verify its behavior in a real application context.


}
