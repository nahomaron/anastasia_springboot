package com.anastasia.Anastasia_BackEnd.core.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SesSnsMessage(
        @JsonProperty("Type")
        String type,
        @JsonProperty("MessageId")
        String messageId,
        @JsonProperty("Message")
        String message,
        @JsonProperty("Timestamp")
        String timestamp,
        @JsonProperty("TopicArn")
        String topicArn,
        @JsonProperty("Subject")
        String subject,
        @JsonProperty("Token")
        String token,
        @JsonProperty("SubscribeURL")
        String subscribeURL,
        @JsonProperty("SignatureVersion")
        String signatureVersion,
        @JsonProperty("Signature")
        String signature,
        @JsonProperty("SigningCertURL")
        String signingCertURL
) {
}
