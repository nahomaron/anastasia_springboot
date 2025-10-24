package com.anastasia.Anastasia_BackEnd.modules.payments.web.dto;

import com.anastasia.Anastasia_BackEnd.modules.payments.domain.model.PaymentPurpose;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSubscriptionRequest {
    @NotNull private PaymentPurpose purpose;
    @Min(50) private long amount;
    @NotBlank private String currency;
    private String memberId;
    private String fundId;
}
