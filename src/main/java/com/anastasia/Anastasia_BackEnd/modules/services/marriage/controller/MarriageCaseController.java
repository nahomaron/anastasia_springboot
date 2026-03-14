package com.anastasia.Anastasia_BackEnd.modules.services.marriage.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageAdminInitiationRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageCaseResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageCounterpartPlaceholderRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageDocumentMetadataRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageDocumentResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriageMemberInitiationRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriagePairingAcceptRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriagePairingTokenCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriagePairingTokenResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriagePartyApplicationResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriagePartyDraftRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.model.MarriagePartyRole;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.service.MarriageCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/marriage-cases")
@RequiresTenantFeature(TenantFeature.SACRAMENTAL_SERVICES)
public class MarriageCaseController {

    private final MarriageCaseService marriageCaseService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/member-initiation")
    public ResponseEntity<MarriageCaseResponse> startMemberInitiatedCase(
            @Valid @RequestBody MarriageMemberInitiationRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.startMemberInitiatedCase(request), HttpStatus.CREATED);
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping
    public ResponseEntity<MarriageCaseResponse> createAdminInitiatedCase(
            @Valid @RequestBody MarriageAdminInitiationRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.createAdminInitiatedCase(request), HttpStatus.CREATED);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mine")
    public ResponseEntity<List<MarriageCaseResponse>> listMine() {
        return ResponseEntity.ok(marriageCaseService.listMine());
    }

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/access")
    public ResponseEntity<List<MarriageCaseResponse>> listAccessibleCases() {
        return ResponseEntity.ok(marriageCaseService.listAccessibleCases());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{caseId}")
    public ResponseEntity<MarriageCaseResponse> getCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.getCase(caseId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{caseId}/counterpart-placeholder/{partyRole}")
    public ResponseEntity<MarriageCaseResponse> createCounterpartPlaceholder(
            @PathVariable UUID caseId,
            @PathVariable MarriagePartyRole partyRole,
            @Valid @RequestBody MarriageCounterpartPlaceholderRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.createCounterpartPlaceholder(caseId, partyRole, request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{caseId}/pairing-token")
    public ResponseEntity<MarriagePairingTokenResponse> createPairingToken(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriagePairingTokenCreateRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.createPairingToken(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/pairing/accept")
    public ResponseEntity<MarriageCaseResponse> acceptPairing(@Valid @RequestBody MarriagePairingAcceptRequest request) {
        return ResponseEntity.ok(marriageCaseService.acceptPairing(request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{caseId}/parties/{partyRole}")
    public ResponseEntity<MarriagePartyApplicationResponse> getPartyApplication(
            @PathVariable UUID caseId,
            @PathVariable MarriagePartyRole partyRole
    ) {
        return ResponseEntity.ok(marriageCaseService.getPartyApplication(caseId, partyRole));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{caseId}/parties/{partyRole}/draft")
    public ResponseEntity<MarriagePartyApplicationResponse> savePartyDraft(
            @PathVariable UUID caseId,
            @PathVariable MarriagePartyRole partyRole,
            @Valid @RequestBody MarriagePartyDraftRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.savePartyDraft(caseId, partyRole, request));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{caseId}/parties/{partyRole}/submit")
    public ResponseEntity<MarriagePartyApplicationResponse> submitPartyApplication(
            @PathVariable UUID caseId,
            @PathVariable MarriagePartyRole partyRole,
            @Valid @RequestBody MarriagePartyDraftRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.submitPartyApplication(caseId, partyRole, request));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{caseId}/documents")
    public ResponseEntity<List<MarriageDocumentResponse>> listDocuments(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listDocuments(caseId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{caseId}/documents")
    public ResponseEntity<MarriageDocumentResponse> addDocument(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageDocumentMetadataRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.addDocument(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{caseId}/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID caseId, @PathVariable UUID documentId) {
        marriageCaseService.deleteDocument(caseId, documentId);
        return ResponseEntity.noContent().build();
    }
}
