package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardDownloadPayload;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardSummaryResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardTemplateResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.card.MembershipCardVerifyResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.card.MembershipCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/membership-cards")
public class MembershipCardController {

    private final MembershipCardService membershipCardService;

    @PreAuthorize("hasAnyRole('USER', 'MEMBER', 'OWNER', 'ADMIN', 'PRIEST')")
    @GetMapping("/me")
    public ResponseEntity<MembershipCardSummaryResponse> currentUserCard() {
        return ResponseEntity.ok(membershipCardService.getCurrentUserCardSummary());
    }

    @PreAuthorize("hasAnyRole('USER', 'MEMBER', 'OWNER', 'ADMIN', 'PRIEST')")
    @GetMapping("/me/download")
    public ResponseEntity<byte[]> downloadCurrentUserCard(
            @RequestParam(value = "format", required = false, defaultValue = "pdf") String format
    ) {
        MembershipCardDownloadPayload payload = membershipCardService.downloadCurrentUserCard(format);
        return asAttachment(payload);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'VIEW_MEMBERS')")
    @GetMapping("/members/{memberId}/download")
    public ResponseEntity<byte[]> downloadMemberCard(
            @PathVariable Long memberId,
            @RequestParam(value = "format", required = false, defaultValue = "pdf") String format
    ) {
        MembershipCardDownloadPayload payload = membershipCardService.downloadByMemberId(memberId, format);
        return asAttachment(payload);
    }

    @GetMapping("/verify/{token}")
    public ResponseEntity<MembershipCardVerifyResponse> verify(@PathVariable String token) {
        return ResponseEntity.ok(membershipCardService.verifyToken(token));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS')")
    @GetMapping("/templates")
    public ResponseEntity<List<MembershipCardTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(membershipCardService.listTemplatesForCurrentTenant());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS')")
    @PatchMapping("/templates/{templateId}/default")
    public ResponseEntity<MembershipCardTemplateResponse> setDefaultTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(membershipCardService.setDefaultTemplateForCurrentTenant(templateId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS', 'APPROVE_MEMBERSHIP')")
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
}
