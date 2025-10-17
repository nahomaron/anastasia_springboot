package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.api.config.PostgresTestContainer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
public class EventControllerIT extends PostgresTestContainer {
    // This class is used for integration tests of the EventController.
    // It will contain test methods that interact with the EventController
    // and verify its behavior in a real application context.


}
