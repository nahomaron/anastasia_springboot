package com.anastasia.Anastasia_BackEnd.modules.services.marriage.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.*;
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
public class MarriageReviewController {

    private final MarriageCaseService marriageCaseService;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/reviews/secretary/return")
    public ResponseEntity<MarriageCaseResponse> secretaryReturnForCorrection(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageReviewActionRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.secretaryReturnForCorrection(caseId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/reviews/secretary/approve-civil-checks")
    public ResponseEntity<MarriageCaseResponse> secretaryApproveCivilChecks(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageReviewActionRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.secretaryApproveCivilChecks(caseId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/reviews/admin/hold")
    public ResponseEntity<MarriageCaseResponse> adminHold(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageReviewActionRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.adminHold(caseId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/reviews/admin/return")
    public ResponseEntity<MarriageCaseResponse> adminReturnForCorrection(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageReviewActionRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.adminReturnForCorrection(caseId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/reviews/admin/reject")
    public ResponseEntity<MarriageCaseResponse> adminReject(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageReviewActionRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.adminReject(caseId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/reviews/admin/approve")
    public ResponseEntity<MarriageCaseResponse> adminApprove(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageReviewActionRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.adminApprove(caseId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/confessor-approvals")
    public ResponseEntity<List<MarriageConfessorApprovalResponse>> listConfessorApprovals(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listConfessorApprovals(caseId));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/confessor-approvals/in-app")
    public ResponseEntity<MarriageConfessorApprovalResponse> recordInAppConfessorApproval(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageConfessorApprovalRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.recordInAppConfessorApproval(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/confessor-approvals/external")
    public ResponseEntity<MarriageConfessorApprovalResponse> recordExternalConfessorApproval(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageExternalConfessorApprovalRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.recordExternalConfessorApproval(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/confessor-approvals/block")
    public ResponseEntity<MarriageConfessorApprovalResponse> recordConfessorBlock(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageConfessorBlockRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.recordConfessorBlock(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/confessor-approvals/diocese-override")
    public ResponseEntity<MarriageConfessorApprovalResponse> recordDioceseOverride(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageDioceseOverrideRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.recordDioceseOverride(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/impediments")
    public ResponseEntity<List<MarriageImpedimentResponse>> listImpediments(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listImpediments(caseId));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/impediments")
    public ResponseEntity<MarriageImpedimentResponse> createImpediment(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageImpedimentCreateRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.createImpediment(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/impediments/{impedimentId}/resolve")
    public ResponseEntity<MarriageImpedimentResponse> resolveImpediment(
            @PathVariable UUID caseId,
            @PathVariable UUID impedimentId,
            @Valid @RequestBody MarriageImpedimentResolveRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.resolveImpediment(caseId, impedimentId, request));
    }
}
