package com.anastasia.Anastasia_BackEnd.core.notification.controller;

import com.anastasia.Anastasia_BackEnd.core.notification.service.SesNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
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
        sesNotificationService.handleSnsMessage(rawBody, snsMessageType);
        return ResponseEntity.ok().build();
    }
}
