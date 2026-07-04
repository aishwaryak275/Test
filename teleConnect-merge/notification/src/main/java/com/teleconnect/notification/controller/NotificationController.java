package com.teleconnect.notification.controller;

import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.request.StatusUpdateRequest;
import com.teleconnect.notification.dto.response.NotificationSummaryResponse;
import com.teleconnect.notification.service.NotificationService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/teleConnect/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuditClient auditClient;

    public NotificationController(NotificationService notificationService, AuditClient auditClient) {
        this.notificationService = notificationService;
        this.auditClient = auditClient;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_NOTIFICATION')")
    public ResponseEntity<Map<String, String>> createNotification(
            @Valid @RequestBody NotificationRequest req,
            HttpServletRequest httpReq) {
        ResponseEntity<Map<String, String>> response = ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createNotificationFromRequest(req));
        auditClient.record(AuditAction.CREATE_NOTIFICATION, AuditModule.NOTIFICATION, httpReq);
        return response;
    }

    // ── FETCH ALL ─────────────────────────────────────────────────────────────

    @GetMapping("/fetchAll/{userId}")
    public ResponseEntity<NotificationSummaryResponse> fetchAll(
            @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.fetchAll(userId));
    }

    // ── FETCH UNREAD ──────────────────────────────────────────────────────────

    @GetMapping("/fetchUnread/{userId}")
    public ResponseEntity<Map<String, Object>> fetchUnread(
            @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.fetchUnread(userId));
    }

    // ── FETCH BY CATEGORY ─────────────────────────────────────────────────────

    @GetMapping("/fetchByCategory/{userId}/{category}")
    public ResponseEntity<Map<String, Object>> fetchByCategory(
            @PathVariable Long userId,
            @PathVariable String category) {
        return ResponseEntity.ok(notificationService.fetchByCategory(userId, category));
    }

    // ── UNREAD COUNT (bell badge) ─────────────────────────────────────────────

    @GetMapping("/unreadCount/{userId}")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    // ── UPDATE STATUS (single) ────────────────────────────────────────────────

    @PatchMapping("/updateStatus/{notificationId}")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long notificationId,
            @Valid @RequestBody StatusUpdateRequest req,
            HttpServletRequest httpReq) {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(notificationService.updateStatus(notificationId, req));
        auditClient.record(AuditAction.MARK_NOTIFICATION_READ, AuditModule.NOTIFICATION, httpReq);
        return response;
    }

    // ── MARK ALL READ ─────────────────────────────────────────────────────────

    @PatchMapping("/markAllRead/{userId}")
    public ResponseEntity<Map<String, Object>> markAllRead(
            @PathVariable Long userId,
            HttpServletRequest httpReq) {
        ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(notificationService.markAllRead(userId));
        auditClient.record(AuditAction.MARK_ALL_NOTIFICATIONS_READ, AuditModule.NOTIFICATION, httpReq);
        return response;
    }

    // ── DELETE SINGLE ─────────────────────────────────────────────────────────

    @DeleteMapping("/delete/{notificationId}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long notificationId,
            HttpServletRequest httpReq) {
        var result = notificationService.deleteNotification(notificationId);
        auditClient.record(AuditAction.DELETE_NOTIFICATION, AuditModule.NOTIFICATION, httpReq);
        return ResponseEntity.ok(result);
    }

    // ── DELETE ALL for user ───────────────────────────────────────────────────

    @DeleteMapping("/deleteAll/{userId}")
    public ResponseEntity<Map<String, String>> deleteAll(
            @PathVariable Long userId,
            HttpServletRequest httpReq) {
        var result = notificationService.deleteAllForUser(userId);
        auditClient.record(AuditAction.DELETE_ALL_NOTIFICATIONS, AuditModule.NOTIFICATION, httpReq);
        return ResponseEntity.ok(result);
    }
}
