package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SesSnsMessage(
        @JsonProperty("Type")
        String type,
        @JsonProperty("Message")
        String message,
        @JsonProperty("SubscribeURL")
        String subscribeURL,
        @JsonProperty("SigningCertURL")
        String signingCertURL
) {
}
