package com.anastasia.Anastasia_BackEnd.modules.services.controller;

import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestCreateRequest;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.BaptismServiceRequestResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.dto.MemberServiceRequestListItemResponse;
import com.anastasia.Anastasia_BackEnd.modules.services.service.BaptismRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member-service-requests")
@RequiresTenantFeature(TenantFeature.SACRAMENTAL_SERVICES)
public class MemberServiceRequestController {

    private final BaptismRequestService baptismRequestService;

    @PreAuthorize("hasAnyRole('MEMBER', 'USER', 'PRIMARY_ADMIN', 'ADMIN')")
    @PostMapping("/baptism")
    public ResponseEntity<BaptismServiceRequestResponse> createBaptismRequest(
            @Valid @RequestBody BaptismServiceRequestCreateRequest request
    ) {
        return new ResponseEntity<>(baptismRequestService.create(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('MEMBER', 'USER', 'PRIMARY_ADMIN', 'ADMIN')")
    @GetMapping("/mine")
    public ResponseEntity<List<MemberServiceRequestListItemResponse>> listMyRequests() {
        return ResponseEntity.ok(baptismRequestService.listMine());
    }
}
