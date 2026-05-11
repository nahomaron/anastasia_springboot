package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.service;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionReason;
import com.anastasia.Anastasia_BackEnd.core.notification.service.EmailSuppressionService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.SesNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void handlesSubscriptionConfirmationByCallingSubscribeUrl() {
        SesNotificationService sesNotificationService =
                new SesNotificationService(new ObjectMapper(), restOperations, emailSuppressionService);
        String rawBody = """
                {
                  "Type": "SubscriptionConfirmation",
                  "SubscribeURL": "https://sns.us-east-1.amazonaws.com/confirm"
                }
                """;
        when(restOperations.getForEntity("https://sns.us-east-1.amazonaws.com/confirm", String.class))
                .thenReturn(ResponseEntity.ok("ok"));

        sesNotificationService.handleSnsMessage(rawBody, "SubscriptionConfirmation");

        verify(restOperations).getForEntity("https://sns.us-east-1.amazonaws.com/confirm", String.class);
    }

    @Test
    void suppressesBouncedRecipients() {
        SesNotificationService sesNotificationService =
                new SesNotificationService(new ObjectMapper(), restOperations, emailSuppressionService);
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
        SesNotificationService sesNotificationService =
                new SesNotificationService(new ObjectMapper(), restOperations, emailSuppressionService);
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
    void ignoresSubscriptionConfirmationWithUntrustedUrl() {
        SesNotificationService sesNotificationService =
                new SesNotificationService(new ObjectMapper(), restOperations, emailSuppressionService);
        String rawBody = """
                {
                  "Type": "SubscriptionConfirmation",
                  "SubscribeURL": "http://malicious.example.com/confirm"
                }
                """;

        sesNotificationService.handleSnsMessage(rawBody, "SubscriptionConfirmation");

        verify(restOperations, never()).getForEntity(eq("http://malicious.example.com/confirm"), eq(String.class));
    }

    @Test
    void ignoresSubscriptionConfirmationWithUntrustedSigningCertUrl() {
        SesNotificationService sesNotificationService =
                new SesNotificationService(new ObjectMapper(), restOperations, emailSuppressionService);
        String rawBody = """
                {
                  "Type": "SubscriptionConfirmation",
                  "SubscribeURL": "https://sns.us-east-1.amazonaws.com/confirm",
                  "SigningCertURL": "https://evil.example.com/cert.pem"
                }
                """;

        sesNotificationService.handleSnsMessage(rawBody, "SubscriptionConfirmation");

        verify(restOperations, never()).getForEntity(eq("https://sns.us-east-1.amazonaws.com/confirm"), eq(String.class));
    }

    @Test
    void ignoresUnsupportedMessageTypesSafely() {
        SesNotificationService sesNotificationService =
                new SesNotificationService(new ObjectMapper(), restOperations, emailSuppressionService);
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
