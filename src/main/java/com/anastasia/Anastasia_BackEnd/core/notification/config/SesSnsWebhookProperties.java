package com.anastasia.Anastasia_BackEnd.core.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "notification.email.ses-sns")
@Getter
@Setter
public class SesSnsWebhookProperties {

    /**
     * SNS topic ARNs allowed to send SES bounce/complaint notifications.
     * Keep this scoped to the exact SES SNS topics used by the application.
     */
    private List<String> topicArns = new ArrayList<>();
}
