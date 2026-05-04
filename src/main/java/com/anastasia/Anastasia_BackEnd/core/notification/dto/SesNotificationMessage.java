package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SesNotificationMessage(
        String notificationType,
        Bounce bounce,
        Complaint complaint
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bounce(List<Recipient> bouncedRecipients) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Complaint(List<Recipient> complainedRecipients) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recipient(String emailAddress) {
    }
}
