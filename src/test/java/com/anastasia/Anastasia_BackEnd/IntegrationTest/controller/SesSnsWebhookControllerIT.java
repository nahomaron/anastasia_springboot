package com.anastasia.Anastasia_BackEnd.IntegrationTest.controller;

import com.anastasia.Anastasia_BackEnd.AnastasiaBackEndApplication;
import com.anastasia.Anastasia_BackEnd.Api.config.PostgresTestContainer;
import com.anastasia.Anastasia_BackEnd.core.notification.service.SesNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AnastasiaBackEndApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SesSnsWebhookControllerIT extends PostgresTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SesNotificationService sesNotificationService;

    @Test
    void allowsAnonymousSesWebhookPosts() throws Exception {
        String payload = """
                {
                  "Type": "Notification",
                  "Message": "{\\"notificationType\\":\\"Bounce\\",\\"bounce\\":{\\"bouncedRecipients\\":[{\\"emailAddress\\":\\"bounce@simulator.amazonses.com\\"}]}}"
                }
                """;

        mockMvc.perform(post("/api/v1/email/ses-events")
                        .header("x-amz-sns-message-type", "Notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(sesNotificationService).handleSnsMessage(payload, "Notification");
    }
}
