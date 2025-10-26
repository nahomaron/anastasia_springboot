package com.anastasia.Anastasia_BackEnd.service.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account"),
    RESET_PASSWORD("reset_password"),
    WELCOME("welcome"),
    NOTIFICATION("notification"),
    PAYMENT_RECEIPT("payment_receipt"),
    SUBSCRIPTION_ACTIVATED("subscription_activated"),
    SUBSCRIPTION_CANCELED("subscription_canceled");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
