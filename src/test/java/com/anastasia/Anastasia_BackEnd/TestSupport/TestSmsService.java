package com.anastasia.Anastasia_BackEnd.TestSupport;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsService;
import com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service.SmsTemplateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
@Profile({"test", "test-server", "api"})  // Active in test and API automation profiles
public class TestSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TestSmsService.class);
    private final ConcurrentHashMap<String, String> phoneToOtp = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> sendSms(String to, SmsTemplateType type, Map<String, Object> templateProps) {
        log.debug("Mock SMS send to {} using template {} with props {}", to, type, templateProps);
        if (type == SmsTemplateType.OTP && templateProps != null) {
            Object rawCode = templateProps.get("otp_code");
            if (rawCode != null) {
                phoneToOtp.put(to, rawCode.toString());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    public Optional<String> getLastOtpForPhone(String phone) {
        return Optional.ofNullable(phoneToOtp.get(phone));
    }

    public void clearOtp(String phone) {
        phoneToOtp.remove(phone);
    }
}
