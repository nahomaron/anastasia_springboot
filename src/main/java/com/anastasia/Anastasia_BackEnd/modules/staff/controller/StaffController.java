package com.anastasia.Anastasia_BackEnd.modules.staff.controller;

import com.anastasia.Anastasia_BackEnd.modules.staff.dto.CreateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.StaffResponse;
import com.anastasia.Anastasia_BackEnd.modules.staff.dto.UpdateStaffRequest;
import com.anastasia.Anastasia_BackEnd.modules.staff.model.StaffEmploymentStatus;
import com.anastasia.Anastasia_BackEnd.modules.staff.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/staff")
@PreAuthorize(
        "hasAnyRole('OWNER', 'PRIMARY_ADMIN') or " +
        "(hasRole('ADMIN') and @permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'VIEW_STAFF'))"
)
public class StaffController {

    private final StaffService staffService;

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF')")
    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        return new ResponseEntity<>(staffService.create(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'VIEW_STAFF')")
    @GetMapping
    public ResponseEntity<Page<StaffResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StaffEmploymentStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(staffService.list(q, status, pageable));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'VIEW_STAFF')")
    @GetMapping("/{staffId}")
    public ResponseEntity<StaffResponse> getById(@PathVariable Long staffId) {
        return ResponseEntity.ok(staffService.getById(staffId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF')")
    @PatchMapping("/{staffId}")
    public ResponseEntity<StaffResponse> update(
            @PathVariable Long staffId,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        return ResponseEntity.ok(staffService.update(staffId, request));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF')")
    @DeleteMapping("/{staffId}")
    public ResponseEntity<StaffResponse> deactivate(@PathVariable Long staffId) {
        return ResponseEntity.ok(staffService.deactivate(staffId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIMARY_ADMIN') or @permissionEvaluator.hasAny(authentication, 'MANAGE_STAFF', 'RESET_STAFF_CREDENTIALS')")
    @PostMapping("/{staffId}/reset-credentials")
    public ResponseEntity<Void> resetCredentials(@PathVariable Long staffId) {
        staffService.resetCredentials(staffId);
        return ResponseEntity.noContent().build();
    }
}
