package com.anastasia.Anastasia_BackEnd.modules.publiccontact.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Service
public class TurnstileVerificationService {

    private final RestClient restClient;
    private final String secretKey;
    private final String verifyUrl;

    public TurnstileVerificationService(
            RestClient.Builder restClientBuilder,
            @Value("${app.security.turnstile.secret-key:}") String secretKey,
            @Value("${app.security.turnstile.verify-url:https://challenges.cloudflare.com/turnstile/v0/siteverify}") String verifyUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.verifyUrl = verifyUrl;
    }

    public void verify(String token, String remoteIp) {
        if (!StringUtils.hasText(secretKey)) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Turnstile verification is not configured on the server."
            );
        }

        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secretKey);
        body.add("response", token);
        if (StringUtils.hasText(remoteIp)) {
            body.add("remoteip", remoteIp);
        }

        TurnstileVerificationResponse response;
        try {
            response = restClient.post()
                    .uri(verifyUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(TurnstileVerificationResponse.class);
        } catch (Exception ex) {
            log.error("Turnstile siteverify call failed", ex);
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "Turnstile verification is temporarily unavailable."
            );
        }

        if (response == null || !Boolean.TRUE.equals(response.success())) {
            log.warn("Turnstile verification failed with codes={}",
                    response != null ? response.errorCodes() : List.of("missing-response"));
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Human verification failed. Please try again."
            );
        }
    }

    private record TurnstileVerificationResponse(
            Boolean success,
            List<String> errorCodes
    ) {}
}
