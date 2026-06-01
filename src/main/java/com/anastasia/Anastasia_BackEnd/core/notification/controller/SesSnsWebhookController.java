package com.anastasia.Anastasia_BackEnd.core.notification.controller;

import com.anastasia.Anastasia_BackEnd.core.notification.service.SesNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.anastasia.Anastasia_BackEnd.core.notification.service.InvalidSesSnsMessageException;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Slf4j
public class SesSnsWebhookController {

    private final SesNotificationService sesNotificationService;

    /**
     * SNS subscription confirmation is required before SNS sends SES notifications.
     * SES SNS topics must be in the same AWS region as SES.
     * SES simulator addresses can be used for testing:
     * bounce@simulator.amazonses.com
     * complaint@simulator.amazonses.com
     */
    @PostMapping("/ses-events")
    public ResponseEntity<Void> handleSesEvents(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(value = "x-amz-sns-message-type", required = false) String snsMessageType
    ) {
        try {
            sesNotificationService.handleSnsMessage(rawBody, snsMessageType);
            return ResponseEntity.ok().build();
        } catch (InvalidSesSnsMessageException ex) {
            log.warn("Rejected SES SNS webhook: {}", ex.getMessage());
            return ResponseEntity.status(ex.status()).build();
        } catch (Exception ex) {
            log.error("Unexpected SES SNS webhook failure: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
