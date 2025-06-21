package com.anastasia.Anastasia_BackEnd.model.sms;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhoneVerificationRequest {
    private String phone;
    private String otp;
}
