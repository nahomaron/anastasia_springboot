package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.common.utils.RateLimiterService;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardDownloadPayload;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardTemplateResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardVerifyResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/membership-cards")
public class MembershipCardController {

    private static final Duration VERIFY_IP_RATE_LIMIT_PERIOD = Duration.ofMinutes(10);
    private static final Duration VERIFY_TOKEN_RATE_LIMIT_PERIOD = Duration.ofMinutes(10);

    private final MembershipCardService membershipCardService;
    private final RateLimiterService rateLimiterService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<MembershipCardSummaryResponse> currentUserCard() {
        return ResponseEntity.ok(membershipCardService.getCurrentUserCardSummary());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/download")
    public ResponseEntity<byte[]> downloadCurrentUserCard(
            @RequestParam(value = "format", required = false, defaultValue = "pdf") String format
    ) {
        MembershipCardDownloadPayload payload = membershipCardService.downloadCurrentUserCard(format);
        return asAttachment(payload);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_MEMBERS')")
    @GetMapping("/members/{memberId}/download")
    public ResponseEntity<byte[]> downloadMemberCard(
            @PathVariable Long memberId,
            @RequestParam(value = "format", required = false, defaultValue = "pdf") String format
    ) {
        MembershipCardDownloadPayload payload = membershipCardService.downloadByMemberId(memberId, format);
        return asAttachment(payload);
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<?> verify(@PathVariable String token, HttpServletRequest request) {
        if (!consumeVerifyRateLimit(request, token)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many requests, try again later"));
        }
        return ResponseEntity.ok(membershipCardService.verifyToken(token, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS')")
    @GetMapping("/templates")
    public ResponseEntity<List<MembershipCardTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(membershipCardService.listTemplatesForCurrentTenant());
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS')")
    @PatchMapping("/templates/{templateId}/default")
    public ResponseEntity<MembershipCardTemplateResponse> setDefaultTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(membershipCardService.setDefaultTemplateForCurrentTenant(templateId));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'APPROVE_MEMBERSHIP')")
    @PatchMapping("/members/{memberId}/revoke")
    public ResponseEntity<Void> revokeCard(
            @PathVariable Long memberId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        membershipCardService.revokeCardForMember(memberId, reason);
        return ResponseEntity.accepted().build();
    }

    private ResponseEntity<byte[]> asAttachment(MembershipCardDownloadPayload payload) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(payload.fileName()).build().toString())
                .body(payload.content());
    }

    private boolean consumeVerifyRateLimit(HttpServletRequest request, String token) {
        String clientIp = normalizeKeyComponent(extractClientIp(request));
        String tokenKey = toTokenBucketKey(token);

        boolean ipAllowed = rateLimiterService.tryConsume(
                "membership-card:verify:ip:" + clientIp,
                20,
                VERIFY_IP_RATE_LIMIT_PERIOD
        );
        boolean tokenAllowed = rateLimiterService.tryConsume(
                "membership-card:verify:token:" + tokenKey,
                8,
                VERIFY_TOKEN_RATE_LIMIT_PERIOD
        );
        return ipAllowed && tokenAllowed;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "anonymous";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeKeyComponent(String value) {
        if (value == null || value.isBlank()) {
            return "anonymous";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String toTokenBucketKey(String token) {
        String normalizedToken = normalizeKeyComponent(token);
        return Integer.toHexString(normalizedToken.hashCode());
    }
}
