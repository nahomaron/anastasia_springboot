package com.anastasia.Anastasia_BackEnd.service.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account"),
    RESET_PASSWORD("reset_password"),
    WELCOME("welcome"),
    NOTIFICATION("notification");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}
