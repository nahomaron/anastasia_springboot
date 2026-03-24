package com.anastasia.Anastasia_BackEnd.core.notification.channel.sms.service;

import com.anastasia.Anastasia_BackEnd.common.utils.PhoneNumberUtils;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** * SmsServiceImpl.java
    * This service handles sending SMS messages using Twilio.
    * for this to work you have to register your brand to A2P 10DLC to comply with US regulations.
 */
//@Slf4j
@Service
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Value("${twilio.account_sid:}")
    private String accountSid;

    @Value("${twilio.auth_token:}")
    private String authToken;

    @Value("${twilio.phone_number:}")
    private String fromNumber;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @PostConstruct
    void initTwilio() {
        if (!smsEnabled) {
            log.info("SMS notifications are disabled (notification.sms.enabled=false).");
            return;
        }

        if (accountSid == null || accountSid.isBlank()
                || authToken == null || authToken.isBlank()
                || fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio SMS is enabled but required credentials are missing.");
        }

        Twilio.init(accountSid, authToken);
        String sidSuffix = accountSid.length() >= 4
                ? accountSid.substring(accountSid.length() - 4)
                : accountSid;
        log.info("Twilio initialized with accountSid ending in {}", sidSuffix);
    }


    /**
     * Sends an SMS message using Twilio.
     *
     * @param to the recipient's phone number
     * @param type the type of SMS template to use
     * @param props properties for the SMS template
     * @return a CompletableFuture that completes when the SMS is sent
     */
    @Async
    @Override
    public CompletableFuture<Void> sendSms(String to,
                                           SmsTemplateType type,
                                           Map<String, Object> props) {

        String body = (type == SmsTemplateType.CUSTOM)
                ? Optional.ofNullable((String) props.get("message_content")).orElse("")
                : type.format(props);

        try {
            String normalizedTo = PhoneNumberUtils.normalize(to);
            log.info("Sending SMS to {}", PhoneNumberUtils.mask(normalizedTo));
            Message response = Message.creator(
                    new PhoneNumber(normalizedTo),
                    new PhoneNumber(fromNumber),
                    body
            ).create();

            log.info("Sent SMS SID {} to {}", response.getSid(), PhoneNumberUtils.mask(normalizedTo));
            return CompletableFuture.completedFuture(null);
        } catch (ApiException ex) {       // Twilio specific runtime ex
            log.error("Twilio error {} while sending to {}", ex.getCode(), PhoneNumberUtils.mask(to), ex);
            throw ex;                     // bubble up so callers can react
        }
    }
}
