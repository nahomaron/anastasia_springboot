package com.anastasia.Anastasia_BackEnd.modules.users.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TotpSetupResponse {
    String secret;
    String otpauthUri;
}
