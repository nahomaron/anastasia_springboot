package com.anastasia.Anastasia_BackEnd.service.sms;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.phone-number}")
    private String fromNumber;


    @PostConstruct
    void initTwilio() {
        if (fromNumber.isBlank()) {
            throw new IllegalStateException("twilio.phone-number must be configured");
        }

        Twilio.init(accountSid, authToken);
        log.info("Twilio initialized with accountSid ending in {}", accountSid.substring(accountSid.length() - 4));
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
            log.info("Sending SMS to: {}", to);
            Message response = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber),
                    body
            ).create();

            log.info("Sent SMS SID {} → {}", response.getSid(), to);
            return CompletableFuture.completedFuture(null);
        } catch (ApiException ex) {       // Twilio specific runtime ex
            log.error("Twilio error {} while sending to {}", ex.getCode(), to, ex);
            throw ex;                     // bubble up so callers can react
        }
    }
}