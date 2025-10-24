package com.anastasia.Anastasia_BackEnd.modules.payments.web.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SubscriptionResponse {
    private UUID subscriptionId;
    private String status;
    private String checkoutUrl;
}
