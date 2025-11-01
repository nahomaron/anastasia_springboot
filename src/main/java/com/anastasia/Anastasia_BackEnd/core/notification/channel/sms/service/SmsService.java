package com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface SmsService {
    CompletableFuture<Void> sendSms(String to,
                                    SmsTemplateType type,
                                    Map<String, Object> templateProps);
}