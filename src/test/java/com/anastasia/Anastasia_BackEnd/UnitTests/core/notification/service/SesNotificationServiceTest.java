package com.anastasia.Anastasia_BackEnd.UnitTests.core.notification.service;

import com.anastasia.Anastasia_BackEnd.UnitTests.support.LenientMockitoTest;
import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionReason;
import com.anastasia.Anastasia_BackEnd.core.notification.service.EmailSuppressionService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.InvalidSesSnsMessageException;
import com.anastasia.Anastasia_BackEnd.core.notification.service.SesNotificationService;
import com.anastasia.Anastasia_BackEnd.core.notification.service.SesSnsMessageVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestOperations;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@LenientMockitoTest
class SesNotificationServiceTest {

    @Mock
    private RestOperations snsRestOperations;

    @Mock
    private EmailSuppressionService emailSuppressionService;

    @Mock
    private SesSnsMessageVerifier sesSnsMessageVerifier;

    private SesNotificationService sesNotificationService;

    @BeforeEach
    void setUp() {
        sesNotificationService = new SesNotificationService(
                new ObjectMapper(),
                snsRestOperations,
                emailSuppressionService,
                sesSnsMessageVerifier
        );
    }

    @Test
    void handleSnsMessage_shouldSuppressRecipientsForVerifiedBounceNotifications() {
        String payload = """
                {
                  "Type": "Notification",
                  "MessageId": "84a33e93-1cf0-4f2d-92ea-57ea15915dac",
                  "TopicArn": "arn:aws:sns:us-east-1:123456789012:ses-events",
                  "Timestamp": "2026-05-29T16:30:00.000Z",
                  "SignatureVersion": "2",
                  "Signature": "ignored-by-mock",
                  "SigningCertURL": "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-test.pem",
                  "Message": "{\\"notificationType\\":\\"Bounce\\",\\"bounce\\":{\\"bouncedRecipients\\":[{\\"emailAddress\\":\\"member@example.com\\"}]}}"
                }
                """;

        sesNotificationService.handleSnsMessage(payload, "Notification");

        verify(emailSuppressionService).markSuppressed(
                "member@example.com",
                EmailSuppressionReason.BOUNCE,
                "Bounce"
        );
    }

    @Test
    void handleSnsMessage_shouldRejectUnverifiedNotificationsBeforeSuppression() {
        String payload = """
                {
                  "Type": "Notification",
                  "MessageId": "84a33e93-1cf0-4f2d-92ea-57ea15915dac",
                  "TopicArn": "arn:aws:sns:us-east-1:123456789012:ses-events",
                  "Timestamp": "2026-05-29T16:30:00.000Z",
                  "SignatureVersion": "2",
                  "Signature": "forged",
                  "SigningCertURL": "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-test.pem",
                  "Message": "{\\"notificationType\\":\\"Complaint\\",\\"complaint\\":{\\"complainedRecipients\\":[{\\"emailAddress\\":\\"member@example.com\\"}]}}"
                }
                """;

        doThrow(new InvalidSesSnsMessageException(HttpStatus.FORBIDDEN, "SNS signature verification failed"))
                .when(sesSnsMessageVerifier)
                .verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("Notification"));

        assertThatThrownBy(() -> sesNotificationService.handleSnsMessage(payload, "Notification"))
                .isInstanceOf(InvalidSesSnsMessageException.class)
                .extracting(ex -> ((InvalidSesSnsMessageException) ex).status())
                .isEqualTo(HttpStatus.FORBIDDEN);

        verify(emailSuppressionService, never()).markSuppressed(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
