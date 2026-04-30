package com.parkease.notification.web.resource;

import com.parkease.notification.service.NotificationService;
import com.parkease.notification.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications — send, read, delete")
public class NotificationResource {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send a single notification — ADMIN")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.send(request));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send bulk notifications — ADMIN")
    public ResponseEntity<List<NotificationResponse>> sendBulk(
            @Valid @RequestBody BulkNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.sendBulk(request));
    }

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Get all notifications for a recipient (paginated)")
    public ResponseEntity<Page<NotificationResponse>> getByRecipient(
            @PathVariable Long recipientId,
            @RequestHeader("X-User-Id") Long callerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        enforceOwnership(callerId, recipientId, auth);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return ResponseEntity.ok(notificationService.getByRecipient(recipientId, pageable));
    }

    @GetMapping("/recipient/{recipientId}/unread")
    @Operation(summary = "Get unread notifications for a recipient (paginated)")
    public ResponseEntity<Page<NotificationResponse>> getUnread(
            @PathVariable Long recipientId,
            @RequestHeader("X-User-Id") Long callerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        enforceOwnership(callerId, recipientId, auth);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return ResponseEntity.ok(notificationService.getUnread(recipientId, pageable));
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long recipientId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        enforceOwnership(callerId, recipientId, auth);
        return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(recipientId)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        boolean isAdmin = hasRole(auth, "ADMIN");
        notificationService.markAsRead(notificationId, callerId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/recipient/{recipientId}/read-all")
    @Operation(summary = "Mark all notifications as read for a recipient")
    public ResponseEntity<Void> markAllRead(
            @PathVariable Long recipientId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        enforceOwnership(callerId, recipientId, auth);
        notificationService.markAllRead(recipientId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> delete(
            @PathVariable Long notificationId,
            @RequestHeader("X-User-Id") Long callerId,
            Authentication auth) {
        boolean isAdmin = hasRole(auth, "ADMIN");
        notificationService.deleteNotification(notificationId, callerId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/recipient/{recipientId}/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete all notifications for a recipient — ADMIN")
    public ResponseEntity<Void> deleteAllByRecipient(@PathVariable Long recipientId) {
        notificationService.deleteAllByRecipient(recipientId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/email")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send a direct email — ADMIN")
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody Map<String, String> body) {
        String to      = body.get("to");
        String subject = body.get("subject");
        String content = body.get("body");
        if (to == null || subject == null || content == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "to, subject, body are required"));
        }
        notificationService.sendEmail(to, subject, content);
        return ResponseEntity.ok(Map.of("message", "Email sent to " + to));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all notifications — ADMIN")
    public ResponseEntity<List<NotificationResponse>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    private void enforceOwnership(Long callerId, Long recipientId, Authentication auth) {
        if (!hasRole(auth, "ADMIN") && !callerId.equals(recipientId)) {
            throw new AccessDeniedException("You can only access your own notifications");
        }
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
