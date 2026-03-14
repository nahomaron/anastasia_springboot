package com.anastasia.Anastasia_BackEnd.modules.registration.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk.BulkMemberActionResponse;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk.BulkMemberAddToGroupRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.dto.bulk.BulkMemberCommunicationRequest;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.MemberBulkActionService;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registrar/members/bulk")
@RequiresTenantFeature(TenantFeature.MEMBER_MANAGEMENT)
@PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_MEMBERS')")
public class MemberBulkActionController {

    private final MemberBulkActionService memberBulkActionService;

    @PostMapping("/communications")
    public ResponseEntity<BulkMemberActionResponse> sendCommunication(
            @Valid @RequestBody BulkMemberCommunicationRequest request
    ) {
        return ResponseEntity.ok(memberBulkActionService.sendCommunication(request));
    }

    @PostMapping("/add-to-group")
    public ResponseEntity<BulkMemberActionResponse> addToGroup(
            @Valid @RequestBody BulkMemberAddToGroupRequest request
    ) {
        return ResponseEntity.ok(memberBulkActionService.addToGroup(request));
    }
}
