package com.anastasia.Anastasia_BackEnd.modules.payments.web.dto;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateIntentRequest {
    @NotNull private PaymentPurpose purpose;
    @Min(50) private long amount;     // minor units (e.g., >= $0.50)
    @NotBlank private String currency; // "USD"
    private String memberId;
    private String fundId;
}
