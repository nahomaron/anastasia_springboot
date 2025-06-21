package com.anastasia.Anastasia_BackEnd.service.sms;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SmsService {
    CompletableFuture<Void> sendSms(String to,
                                    SmsTemplateType type,
                                    Map<String, Object> templateProps);
}