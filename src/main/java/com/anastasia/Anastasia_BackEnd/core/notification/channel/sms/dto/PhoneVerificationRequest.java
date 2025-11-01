package com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhoneVerificationRequest {
    private String phone;
    private String otp;
}
