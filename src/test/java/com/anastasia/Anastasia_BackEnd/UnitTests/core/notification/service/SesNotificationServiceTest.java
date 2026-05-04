package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.service;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionReason;
import com.anastasia.Anastasia_BackEnd.core.notification.service.EmailSuppressionService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.SesNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LenientMockitoTest
class SesNotificationServiceTest {

    @Mock
    private RestOperations restOperations;

    @Mock
    private EmailSuppressionService emailSuppressionService;

    private SesNotificationService sesNotificationService;

    @BeforeEach
    void setUp() {
        sesNotificationService = new SesNotificationService(new ObjectMapper(), restOperations, emailSuppressionService);
    }

    @Test
    void handlesSubscriptionConfirmationByCallingSubscribeUrl() {
        String rawBody = """
                {
                  "Type": "SubscriptionConfirmation",
                  "SubscribeURL": "https://sns.example.com/confirm"
                }
                """;
        when(restOperations.getForEntity("https://sns.example.com/confirm", String.class))
                .thenReturn(ResponseEntity.ok("ok"));

        sesNotificationService.handleSnsMessage(rawBody, "SubscriptionConfirmation");

        verify(restOperations).getForEntity("https://sns.example.com/confirm", String.class);
    }

    @Test
    void suppressesBouncedRecipients() {
        String rawBody = """
                {
                  "Type": "Notification",
                  "Message": "{\\"notificationType\\":\\"Bounce\\",\\"bounce\\":{\\"bouncedRecipients\\":[{\\"emailAddress\\":\\"first@example.com\\"},{\\"emailAddress\\":\\"second@example.com\\"}]}}"
                }
                """;

        sesNotificationService.handleSnsMessage(rawBody, "Notification");

        verify(emailSuppressionService).markSuppressed("first@example.com", EmailSuppressionReason.BOUNCE, "Bounce");
        verify(emailSuppressionService).markSuppressed("second@example.com", EmailSuppressionReason.BOUNCE, "Bounce");
    }

    @Test
    void suppressesComplainedRecipients() {
        String rawBody = """
                {
                  "Type": "Notification",
                  "Message": "{\\"notificationType\\":\\"Complaint\\",\\"complaint\\":{\\"complainedRecipients\\":[{\\"emailAddress\\":\\"complaint@example.com\\"}]}}"
                }
                """;

        sesNotificationService.handleSnsMessage(rawBody, "Notification");

        verify(emailSuppressionService)
                .markSuppressed("complaint@example.com", EmailSuppressionReason.COMPLAINT, "Complaint");
    }

    @Test
    void ignoresUnsupportedMessageTypesSafely() {
        String rawBody = """
                {
                  "Type": "UnsubscribeConfirmation"
                }
                """;

        sesNotificationService.handleSnsMessage(rawBody, "UnsubscribeConfirmation");

        verify(restOperations, never()).getForEntity(eq("https://sns.example.com/confirm"), eq(String.class));
        verify(emailSuppressionService, never()).markSuppressed(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }
}
