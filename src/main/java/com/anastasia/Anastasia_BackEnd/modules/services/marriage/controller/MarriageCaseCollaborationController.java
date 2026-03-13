package com.anastasia.Anastasia_BackEnd.modules.services.marriage.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageAuditEventResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageCaseNoteRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageCaseNoteResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageReviewResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageStatusHistoryResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.service.MarriageCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/marriage-cases")
@RequiresTenantFeature(TenantFeature.SACRAMENTAL_SERVICES)
public class MarriageCaseCollaborationController {

    private final MarriageCaseService marriageCaseService;

    @PreAuthorize("hasAnyRole('MEMBER', 'USER', 'STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/notes")
    public ResponseEntity<List<MarriageCaseNoteResponse>> listNotes(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listNotes(caseId));
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'USER', 'STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/notes")
    public ResponseEntity<MarriageCaseNoteResponse> addNote(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageCaseNoteRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.addNote(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'USER', 'STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/history")
    public ResponseEntity<List<MarriageStatusHistoryResponse>> listStatusHistory(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listStatusHistory(caseId));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/audit-events")
    public ResponseEntity<List<MarriageAuditEventResponse>> listAuditEvents(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listAuditEvents(caseId));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/reviews")
    public ResponseEntity<List<MarriageReviewResponse>> listReviews(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listReviews(caseId));
    }
}
