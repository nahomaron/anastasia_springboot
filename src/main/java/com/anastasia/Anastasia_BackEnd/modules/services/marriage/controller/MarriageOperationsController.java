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
public class MarriageOperationsController {

    private final MarriageCaseService marriageCaseService;

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/payments")
    public ResponseEntity<List<MarriageManualPaymentResponse>> listPayments(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listPayments(caseId));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/payments/manual")
    public ResponseEntity<MarriageManualPaymentResponse> recordManualPayment(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageManualPaymentRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.recordManualPayment(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/payments/{paymentId}/verify")
    public ResponseEntity<MarriageManualPaymentResponse> verifyManualPayment(
            @PathVariable UUID caseId,
            @PathVariable UUID paymentId,
            @Valid @RequestBody MarriageManualPaymentVerificationRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.verifyManualPayment(caseId, paymentId, request));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN', 'PRIEST') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/{caseId}/witnesses")
    public ResponseEntity<List<MarriageWitnessResponse>> listWitnesses(@PathVariable UUID caseId) {
        return ResponseEntity.ok(marriageCaseService.listWitnesses(caseId));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/witnesses")
    public ResponseEntity<MarriageWitnessResponse> addWitness(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageWitnessUpsertRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.addWitness(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PutMapping("/{caseId}/witnesses/{witnessId}")
    public ResponseEntity<MarriageWitnessResponse> updateWitness(
            @PathVariable UUID caseId,
            @PathVariable UUID witnessId,
            @Valid @RequestBody MarriageWitnessUpsertRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.updateWitness(caseId, witnessId, request));
    }

    @PreAuthorize("hasAnyRole('STAFF', 'OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @DeleteMapping("/{caseId}/witnesses/{witnessId}")
    public ResponseEntity<Void> deleteWitness(@PathVariable UUID caseId, @PathVariable UUID witnessId) {
        marriageCaseService.deleteWitness(caseId, witnessId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/priest-assignment")
    public ResponseEntity<MarriagePriestAssignmentResponse> assignPriest(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriagePriestAssignmentRequest request
    ) {
        return new ResponseEntity<>(marriageCaseService.assignPriest(caseId, request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/schedule/propose")
    public ResponseEntity<MarriageScheduleResponse> proposeSchedule(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageScheduleRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.proposeSchedule(caseId, request));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/schedule/confirm")
    public ResponseEntity<MarriageScheduleResponse> confirmSchedule(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageScheduleRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.confirmSchedule(caseId, request));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/schedule/reschedule")
    public ResponseEntity<MarriageScheduleResponse> reschedule(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageScheduleRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.reschedule(caseId, request));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN', 'ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @PostMapping("/{caseId}/schedule/cancel")
    public ResponseEntity<MarriageScheduleResponse> cancelSchedule(
            @PathVariable UUID caseId,
            @Valid @RequestBody MarriageScheduleCancellationRequest request
    ) {
        return ResponseEntity.ok(marriageCaseService.cancelSchedule(caseId, request));
    }
}
