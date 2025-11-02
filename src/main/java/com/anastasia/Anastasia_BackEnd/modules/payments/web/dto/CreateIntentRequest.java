package com.anastasia.Anastasia_BackEnd.modules.payments.web.dto;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateIntentRequest {
    @NotNull private PaymentPurpose purpose;
    @Min(50) private long amount;     // minor units (e.g., >= $0.50)
    @NotBlank private String currency; // "USD"
    private Long memberId;
    private UUID userId;
    @Email private String userEmail;
    private String fundId;
}
