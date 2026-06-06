package com.anastasia.Anastasia_BackEnd.modules.publiccontact.controller;

import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.dto.PublicContactRequest;
import com.anastasia.Anastasia_BackEnd.modules.publiccontact.service.PublicContactService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class PublicContactController {

    private final PublicContactService publicContactService;
    private final RateLimiterService rateLimiterService;

    @PostMapping(
            path = "/contact-requests",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, String>> submit(
            @Valid @ModelAttribute PublicContactRequest request,
            HttpServletRequest httpRequest
    ) {
        String remoteIp = httpRequest.getRemoteAddr();
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String rateLimitKey = "public:contact:" + remoteIp + ":" + normalizedEmail;

        if (!rateLimiterService.tryConsume(rateLimitKey, 5L, Duration.ofMinutes(15))) {
            return ResponseEntity.status(429).body(Map.of(
                    "message", "Too many contact requests. Try again later."
            ));
        }

        publicContactService.submit(request, remoteIp);
        return ResponseEntity.ok(Map.of(
                "message", "Contact request submitted successfully."
        ));
    }
}
