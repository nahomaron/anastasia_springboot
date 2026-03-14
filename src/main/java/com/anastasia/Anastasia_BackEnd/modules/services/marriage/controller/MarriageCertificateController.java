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
@RequestMapping("/api/v1")
@RequiresTenantFeature(TenantFeature.SACRAMENTAL_SERVICES)
public class MarriageCertificateController {

    private final MarriageCaseService marriageCaseService;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/marriage-cases/{caseId}/ceremony/complete")
    public ResponseEntity<MarriageCaseResponse> completeCeremony(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageCeremonyCompletionRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.completeCeremony(caseId, request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PutMapping("/marriage-config/certificate-sequence")
    public ResponseEntity<MarriageCertificateSequenceConfigResponse> configureCertificateSequence(
            @Valid @RequestBody MarriageCertificateSequenceConfigRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.configureCertificateSequence(request));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/marriage-config/certificate-sequence")
    public ResponseEntity<MarriageCertificateSequenceConfigResponse> getCertificateSequenceConfig(
            @RequestParam("churchNumber") String churchNumber
    ) {
        return ResponseEntity.ok(marriageCaseService.getCertificateSequenceConfig(churchNumber));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/marriage-cases/{caseId}/certificate/prepare")
    public ResponseEntity<MarriageCertificateResponse> prepareCertificate(
            @PathVariable UUID caseId,
            @RequestBody(required = false) MarriageCertificatePrepareRequest request
    ) {
        MarriageCertificatePrepareRequest payload = request == null ? new MarriageCertificatePrepareRequest(null, null) : request;
        return ResponseEntity.ok(marriageCaseService.prepareCertificate(caseId, payload));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/marriage-cases/{caseId}/certificate/issue")
    public ResponseEntity<MarriageCertificateResponse> issueCertificate(
            @PathVariable UUID caseId,
            @RequestBody(required = false) MarriageCertificateIssueRequest request
    ) {
        MarriageCertificateIssueRequest payload = request == null ? new MarriageCertificateIssueRequest(null, null) : request;
        return ResponseEntity.ok(marriageCaseService.issueCertificate(caseId, payload));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/marriage-cases/{caseId}/certificate")
    public ResponseEntity<MarriageCertificateResponse> getCertificate(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.getCertificate(caseId));
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/marriage-certificates/registry")
    public ResponseEntity<List<MarriageCertificateResponse>> listCertificateRegistry() {
        return ResponseEntity.ok(marriageCaseService.listCertificateRegistry());
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/marriage-certificates/{certificateId}/amendments")
    public ResponseEntity<MarriageCertificateAmendmentResponse> createCertificateAmendment(
            @PathVariable UUID certificateId,
            @Valid @RequestBody MarriageCertificateAmendmentRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.createCertificateAmendment(certificateId, request), HttpStatus.CREATED);
    }
}
