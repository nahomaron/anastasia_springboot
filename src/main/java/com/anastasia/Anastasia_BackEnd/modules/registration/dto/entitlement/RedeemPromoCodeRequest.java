package com.anastasia.Anastasia_BackEnd.modules.registration.dto.entitlement;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemPromoCodeRequest {
    @NotBlank
    private String code;
    private String reason;
}
