package com.anastasia.Anastasia_BackEnd.modules.services.marriage.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.dto.MarriagePriestLookupResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.marriage.service.MarriageCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/marriage-lookups")
@RequiresTenantFeature(TenantFeature.SACRAMENTAL_SERVICES)
public class MarriageLookupController {

    private final MarriageCaseService marriageCaseService;

    @PreAuthorize("@permissionEvaluator.hasAny(authentication, 'MANAGE_SERVICES')")
    @GetMapping("/priests/active")
    public ResponseEntity<List<MarriagePriestLookupResponse>> listActivePriests(
            @RequestParam(value = "churchId", required = false) Long churchId,
            @RequestParam(value = "q", required = false) String query
    ) {
        return ResponseEntity.ok(marriageCaseService.listActivePriests(churchId, query));
    }
}
