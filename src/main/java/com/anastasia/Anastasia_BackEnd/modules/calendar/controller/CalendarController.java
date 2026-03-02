package com.anastasia.Anastasia_BackEnd.modules.calendar.controller;

import com.anastasia.Anastasia_BackEnd.core.auth.principal.UserPrincipal;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarEntryRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.CalendarOccurrenceResponse;
import com.anastasia.Anastasia_BackEnd.modules.calendar.dto.OccurrenceOverrideRequest;
import com.anastasia.Anastasia_BackEnd.modules.calendar.model.CalendarEntryType;
import com.anastasia.Anastasia_BackEnd.modules.registration.model.tenant.TenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.registration.service.entitlement.RequiresTenantFeature;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.CalendarEntryService;
import com.anastasia.Anastasia_BackEnd.modules.calendar.service.CalendarOccurrenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
@RequiresTenantFeature(TenantFeature.CALENDAR)
public class CalendarController {

    private final CalendarOccurrenceService occurrenceService;
    private final CalendarEntryService entryService;

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'VIEW_EVENTS', 'MANAGE_EVENTS', 'MANAGE_APPOINTMENT')")
    @GetMapping("/occurrences")
    public ResponseEntity<List<CalendarOccurrenceResponse>> getOccurrences(
            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,
            @RequestParam("end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,
            @RequestParam(value = "types", required = false)
            Set<CalendarEntryType> types
    ) {
        UUID userId = resolveCurrentUserId();
        Set<String> authorities = resolveAuthorities();
        List<CalendarOccurrenceResponse> occurrences = occurrenceService.getOccurrences(
                start,
                end,
                types,
                userId,
                authorities
        );
        return ResponseEntity.ok(occurrences);
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS', 'MANAGE_APPOINTMENT')")
    @PostMapping("/entries")
    public ResponseEntity<?> createEntry(@Valid @RequestBody CalendarEntryRequest request) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(entryService.createEntry(request, userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS', 'MANAGE_APPOINTMENT')")
    @PutMapping("/entries/{entryId}")
    public ResponseEntity<?> updateEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody CalendarEntryRequest request
    ) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(entryService.updateEntry(entryId, request, userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS', 'MANAGE_APPOINTMENT')")
    @PatchMapping(value = "/entries/{entryId}", params = "scope=SERIES")
    public ResponseEntity<?> patchSeries(
            @PathVariable UUID entryId,
            @Valid @RequestBody CalendarEntryRequest request
    ) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(entryService.updateEntry(entryId, request, userId));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS', 'MANAGE_APPOINTMENT')")
    @PatchMapping(value = "/entries/{entryId}", params = "scope=THIS")
    public ResponseEntity<?> patchSingleOccurrence(
            @PathVariable UUID entryId,
            @Valid @RequestBody OccurrenceOverrideRequest request
    ) {
        UUID userId = resolveCurrentUserId();
        entryService.applyOccurrenceOverride(entryId, request, userId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'PRIEST') " +
            "or @permissionEvaluator.hasAny(authentication, 'MANAGE_EVENTS', 'CREATE_EDIT_EVENTS', 'MANAGE_APPOINTMENT')")
    @PatchMapping(value = "/entries/{entryId}", params = "scope=THIS_AND_FUTURE")
    public ResponseEntity<?> patchThisAndFuture(
            @PathVariable UUID entryId,
            @RequestParam("occurrenceDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurrenceDate,
            @Valid @RequestBody CalendarEntryRequest request
    ) {
        UUID userId = resolveCurrentUserId();
        return ResponseEntity.ok(entryService.splitSeries(entryId, occurrenceDate, request, userId));
    }

    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUserUuid();
        }
        throw new IllegalStateException("Unable to resolve the authenticated user");
    }

    private Set<String> resolveAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
    }
}
