package com.anastasia.Anastasia_BackEnd.core.notification.service;

import com.anastasia.Anastasia_BackEnd.core.notification.domain.EmailSuppressionReason;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.SesNotificationMessage;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.SesSnsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SesNotificationService {

    private static final String MESSAGE_TYPE_NOTIFICATION = "Notification";
    private static final String MESSAGE_TYPE_SUBSCRIPTION_CONFIRMATION = "SubscriptionConfirmation";
    private static final String NOTIFICATION_TYPE_BOUNCE = "Bounce";
    private static final String NOTIFICATION_TYPE_COMPLAINT = "Complaint";
    private static final String AWS_HOST_SUFFIX = ".amazonaws.com";

    private final ObjectMapper objectMapper;
    private final RestOperations snsRestOperations;
    private final EmailSuppressionService emailSuppressionService;

    public void handleSnsMessage(String rawBody, String snsMessageTypeHeader) {
        if (!StringUtils.hasText(rawBody)) {
            log.warn("Ignoring SES SNS webhook request with empty body");
            return;
        }

        try {
            SesSnsMessage snsMessage = objectMapper.readValue(rawBody, SesSnsMessage.class);
            String messageType = StringUtils.hasText(snsMessageTypeHeader) ? snsMessageTypeHeader : snsMessage.type();
            if (!StringUtils.hasText(messageType)) {
                log.warn("Ignoring SES SNS webhook request with missing message type");
                return;
            }

            switch (messageType) {
                case MESSAGE_TYPE_SUBSCRIPTION_CONFIRMATION -> handleSubscriptionConfirmation(snsMessage);
                case MESSAGE_TYPE_NOTIFICATION -> handleNotification(snsMessage);
                default -> log.debug("Ignoring unsupported SNS message type={}", messageType);
            }
        } catch (Exception ex) {
            log.warn("Failed to process SES SNS webhook: {}", ex.getMessage());
        }
    }

    private void handleSubscriptionConfirmation(SesSnsMessage snsMessage) {
        if (!StringUtils.hasText(snsMessage.subscribeURL())) {
            log.warn("SNS subscription confirmation missing SubscribeURL");
            return;
        }
        if (!isTrustedAwsUrl(snsMessage.subscribeURL())) {
            log.warn("Ignoring SNS subscription confirmation with untrusted SubscribeURL");
            return;
        }
        if (StringUtils.hasText(snsMessage.signingCertURL()) && !isTrustedAwsUrl(snsMessage.signingCertURL())) {
            log.warn("Ignoring SNS subscription confirmation with untrusted SigningCertURL");
            return;
        }

        try {
            ResponseEntity<String> response = snsRestOperations.getForEntity(snsMessage.subscribeURL(), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Confirmed SNS subscription for SES notifications");
            } else {
                log.warn("SNS subscription confirmation returned status={}", response.getStatusCode().value());
            }
        } catch (Exception ex) {
            log.error("Failed to confirm SNS subscription: {}", ex.getMessage());
        }
    }

    private void handleNotification(SesSnsMessage snsMessage) throws Exception {
        if (!StringUtils.hasText(snsMessage.message())) {
            log.warn("SNS notification missing Message payload");
            return;
        }

        SesNotificationMessage notificationMessage =
                objectMapper.readValue(snsMessage.message(), SesNotificationMessage.class);

        if (NOTIFICATION_TYPE_BOUNCE.equalsIgnoreCase(notificationMessage.notificationType())) {
            suppressRecipients(
                    notificationMessage.bounce() != null
                            ? notificationMessage.bounce().bouncedRecipients()
                            : Collections.emptyList(),
                    EmailSuppressionReason.BOUNCE,
                    notificationMessage.notificationType()
            );
            return;
        }

        if (NOTIFICATION_TYPE_COMPLAINT.equalsIgnoreCase(notificationMessage.notificationType())) {
            suppressRecipients(
                    notificationMessage.complaint() != null
                            ? notificationMessage.complaint().complainedRecipients()
                            : Collections.emptyList(),
                    EmailSuppressionReason.COMPLAINT,
                    notificationMessage.notificationType()
            );
            return;
        }

        log.debug("Ignoring unsupported SES notification type={}", notificationMessage.notificationType());
    }

    private void suppressRecipients(List<SesNotificationMessage.Recipient> recipients,
                                    EmailSuppressionReason reason,
                                    String rawNotificationType) {
        if (recipients == null || recipients.isEmpty()) {
            log.debug("SES notification contained no recipients for reason={}", reason);
            return;
        }

        for (SesNotificationMessage.Recipient recipient : recipients) {
            if (recipient == null || !StringUtils.hasText(recipient.emailAddress())) {
                continue;
            }
            emailSuppressionService.markSuppressed(recipient.emailAddress(), reason, rawNotificationType);
        }
    }

    private boolean isTrustedAwsUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }

            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return normalizedHost.equals("amazonaws.com") || normalizedHost.endsWith(AWS_HOST_SUFFIX);
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring malformed AWS callback URL");
            return false;
        }
    }
}
