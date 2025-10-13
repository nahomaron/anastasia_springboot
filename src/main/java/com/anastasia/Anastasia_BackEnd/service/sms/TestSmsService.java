package com.anastasia.Anastasia_BackEnd.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Primary
@Profile("test")
public class TestSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TestSmsService.class);

    @Override
    public CompletableFuture<Void> sendSms(String to, SmsTemplateType type, Map<String, Object> templateProps) {
        log.debug("Mock SMS send to {} using template {} with props {}", to, type, templateProps);
        return CompletableFuture.completedFuture(null);
    }
}
