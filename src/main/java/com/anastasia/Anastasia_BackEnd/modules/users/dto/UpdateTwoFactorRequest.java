package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTwoFactorRequest {
    private boolean enabled;
}
