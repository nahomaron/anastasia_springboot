package com.anastasia.Anastasia_BackEnd.core.notification.controller;

import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxItemResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationInboxPageResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.NotificationPreferencesResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.UnreadCountResponse;
import com.anastasia.Anastasia_BackEnd.core.notification.dto.UpdateNotificationPreferencesRequest;
import com.anastasia.Anastasia_BackEnd.core.notification.service.NotificationInboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationInboxService notificationInboxService;

    @GetMapping
    public ResponseEntity<NotificationInboxPageResponse> list(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationInboxService.listInbox(status, type, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(new UnreadCountResponse(notificationInboxService.unreadCount()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationInboxItemResponse> markRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationInboxService.markRead(notificationId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(Map.of("updated", notificationInboxService.markAllRead()));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> archive(@PathVariable Long notificationId) {
        notificationInboxService.archive(notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> getPreferences() {
        return ResponseEntity.ok(notificationInboxService.getPreferences());
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdateNotificationPreferencesRequest request
    ) {
        return ResponseEntity.ok(notificationInboxService.updatePreferences(request));
    }
}
