package com.teleconnect.notification.controller;

import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.request.StatusUpdateRequest;
import com.teleconnect.notification.dto.response.MessageResponse;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import com.teleconnect.notification.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teleConnect/notification")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/createNotification")
    @PreAuthorize("hasAuthority('CREATE_NOTIFICATION')")
    public ResponseEntity<MessageResponse> createNotification(
            @RequestBody NotificationRequest request) {

        service.createNotification(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse(
                        "Notification created successfully"));
    }

    @GetMapping("/fetchNotifications/{userId}")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<?> getNotifications(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                service.getNotifications(userId));
    }

    @GetMapping("/fetchNotificationById/{notificationId}")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<?> getNotificationById(
            @PathVariable Long notificationId) {

        return ResponseEntity.ok(
                service.getNotificationById(notificationId));
    }

    @GetMapping("/fetchByStatus/{userId}/{status}")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<?> getByStatus(
            @PathVariable Long userId,
            @PathVariable NotificationStatus status) {

        return ResponseEntity.ok(
                service.getByStatus(userId, status));
    }

    @GetMapping("/fetchByCategory/{userId}/{category}")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<?> getByCategory(
            @PathVariable Long userId,
            @PathVariable NotificationCategory category) {

        return ResponseEntity.ok(
                service.getByCategory(userId, category));
    }

    @GetMapping("/unreadCount/{userId}")
    @PreAuthorize("hasAuthority('VIEW_NOTIFICATIONS')")
    public ResponseEntity<?> getUnreadCount(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                service.getUnreadCount(userId));
    }

    @PutMapping("/markAsRead/{notificationId}")
    @PreAuthorize("hasAuthority('MARK_NOTIFICATIONS')")
    public ResponseEntity<MessageResponse> markAsRead(
            @PathVariable Long notificationId,
            @RequestBody(required = false) StatusUpdateRequest request) {

        service.markAsRead(notificationId);

        return ResponseEntity.ok(
                new MessageResponse(
                        "Notification marked as read"));
    }

    @PutMapping("/dismiss/{notificationId}")
    @PreAuthorize("hasAuthority('MARK_NOTIFICATIONS')")
    public ResponseEntity<MessageResponse> dismissNotification(
            @PathVariable Long notificationId,
            @RequestBody(required = false) StatusUpdateRequest request) {

        service.dismissNotification(notificationId);

        return ResponseEntity.ok(
                new MessageResponse(
                        "Notification dismissed successfully"));
    }

    @PutMapping("/markAllAsRead/{userId}")
    @PreAuthorize("hasAuthority('MARK_NOTIFICATIONS')")
    public ResponseEntity<MessageResponse> markAllAsRead(
            @PathVariable Long userId,
            @RequestBody(required = false) StatusUpdateRequest request) {

        service.markAllAsRead(userId);

        return ResponseEntity.ok(
                new MessageResponse(
                        "All notifications marked as read"));
    }
}