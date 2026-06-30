package com.teleconnect.notification.service;

import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.request.StatusUpdateRequest;
import com.teleconnect.notification.dto.response.NotificationResponse;
import com.teleconnect.notification.dto.response.NotificationSummaryResponse;
import com.teleconnect.notification.entity.Notification;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import com.teleconnect.notification.exception.ResourceNotFoundException;
import com.teleconnect.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    // ── mapper ────────────────────────────────────────────────────────────────

    private NotificationResponse toDTO(Notification n) {
        NotificationResponse dto = new NotificationResponse();
        dto.setNotificationId(n.getNotificationId());
        dto.setUserId(n.getUserId());
        dto.setMessage(n.getMessage());
        dto.setCategory(n.getCategory().name());
        dto.setStatus(n.getStatus().name());
        dto.setCreatedDate(n.getCreatedDate());
        return dto;
    }

    // ── internal helper used by schedulers ───────────────────────────────────

    /**
     * Creates a notification only if an identical UNREAD one does not already exist.
     * Prevents duplicate alerts from repeated scheduler runs.
     */
    public Notification createNotification(Long userId, String message,
                                           NotificationCategory category) {
        boolean exists = notificationRepository.existsByUserIdAndMessageAndStatus(
                userId, message, NotificationStatus.UNREAD);
        if (exists) return null;

        Notification n = new Notification();
        n.setUserId(userId);
        n.setMessage(message);
        n.setCategory(category);
        return notificationRepository.save(n);
    }

    // ── 1. CREATE (manual / via API) ──────────────────────────────────────────

    @Transactional
    public Map<String, String> createNotificationFromRequest(NotificationRequest req) {
        NotificationCategory category;
        try {
            category = NotificationCategory.valueOf(req.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid category: '" + req.getCategory() +
                    "'. Must be USAGE, BILLING, FAULT, PLAN, or COMPLIANCE.");
        }
        createNotification(req.getUserId(), req.getMessage(), category);
        return Map.of("message", "Notification created successfully");
    }

    // ── 2. FETCH ALL for a user ───────────────────────────────────────────────

    public NotificationSummaryResponse fetchAll(Long userId) {
        List<Notification> all = notificationRepository
                .findByUserIdOrderByCreatedDateDesc(userId);
        if (all.isEmpty())
            throw new ResourceNotFoundException(
                    "No notifications found for userId: " + userId);

        long unread = all.stream()
                .filter(n -> n.getStatus() == NotificationStatus.UNREAD)
                .count();

        List<NotificationResponse> dtos = all.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new NotificationSummaryResponse(userId, unread, dtos);
    }

    // ── 3. FETCH UNREAD only ──────────────────────────────────────────────────

    public Map<String, Object> fetchUnread(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndStatusOrderByCreatedDateDesc(
                        userId, NotificationStatus.UNREAD);
        if (unread.isEmpty())
            throw new ResourceNotFoundException(
                    "No unread notifications for userId: " + userId);

        return Map.of(
                "userId", userId,
                "unreadCount", unread.size(),
                "notifications", unread.stream().map(this::toDTO).collect(Collectors.toList())
        );
    }

    // ── 4. FETCH BY CATEGORY ──────────────────────────────────────────────────

    public Map<String, Object> fetchByCategory(Long userId, String categoryStr) {
        NotificationCategory category;
        try {
            category = NotificationCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid category: '" + categoryStr +
                    "'. Must be USAGE, BILLING, FAULT, PLAN, or COMPLIANCE.");
        }

        List<Notification> list = notificationRepository
                .findByUserIdAndCategoryOrderByCreatedDateDesc(userId, category);
        if (list.isEmpty())
            throw new ResourceNotFoundException(
                    "No " + category + " notifications for userId: " + userId);

        return Map.of(
                "userId", userId,
                "category", category.name(),
                "count", list.size(),
                "notifications", list.stream().map(this::toDTO).collect(Collectors.toList())
        );
    }

    // ── 5. UNREAD COUNT (for badge) ───────────────────────────────────────────

    public Map<String, Object> getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndStatus(
                userId, NotificationStatus.UNREAD);
        return Map.of("userId", userId, "unreadCount", count);
    }

    // ── 6. UPDATE STATUS (single) ─────────────────────────────────────────────

    @Transactional
    public Map<String, Object> updateStatus(Long notificationId,
                                            StatusUpdateRequest req) {
        NotificationStatus newStatus;
        try {
            newStatus = NotificationStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status: '" + req.getStatus() +
                    "'. Must be READ or DISMISSED.");
        }

        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found for id: " + notificationId));

        n.setStatus(newStatus);
        notificationRepository.save(n);

        return Map.of(
                "message", "Notification status updated successfully",
                "notificationId", notificationId,
                "newStatus", newStatus.name()
        );
    }

    // ── 7. MARK ALL READ ──────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> markAllRead(Long userId) {
        long existing = notificationRepository.countByUserIdAndStatus(
                userId, NotificationStatus.UNREAD);
        if (existing == 0)
            throw new ResourceNotFoundException(
                    "No unread notifications found for userId: " + userId);

        int updated = notificationRepository.markAllReadForUser(
                userId, NotificationStatus.READ);

        return Map.of(
                "message", "All notifications marked as read",
                "userId", userId,
                "updatedCount", updated
        );
    }

    // ── 8. DELETE SINGLE ──────────────────────────────────────────────────────

    @Transactional
    public Map<String, String> deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId))
            throw new ResourceNotFoundException(
                    "Notification not found for id: " + notificationId);

        notificationRepository.deleteById(notificationId);
        return Map.of("message", "Notification deleted successfully");
    }

    // ── 9. DELETE ALL for user ────────────────────────────────────────────────

    @Transactional
    public Map<String, String> deleteAllForUser(Long userId) {
        long count = notificationRepository.countByUserIdAndStatus(
                userId, NotificationStatus.UNREAD);
        long total = notificationRepository.findByUserIdOrderByCreatedDateDesc(userId).size();
        if (total == 0)
            throw new ResourceNotFoundException(
                    "No notifications found for userId: " + userId);

        notificationRepository.deleteAllByUserId(userId);
        return Map.of("message", "All notifications deleted for userId: " + userId);
    }
}
