package com.teleconnect.notification.service;

import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.request.StatusUpdateRequest;
import com.teleconnect.notification.dto.response.NotificationSummaryResponse;
import com.teleconnect.notification.entity.Notification;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import com.teleconnect.notification.exception.ResourceNotFoundException;
import com.teleconnect.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification buildNotification(Long id, Long userId,
                                            NotificationCategory cat,
                                            NotificationStatus status) {
        Notification n = new Notification();
        n.setNotificationId(id);
        n.setUserId(userId);
        n.setMessage("Test message");
        n.setCategory(cat);
        n.setStatus(status);
        n.setCreatedDate(LocalDateTime.now());
        return n;
    }

    // ── createNotification ────────────────────────────────────────────────────

    @Test
    void createNotification_savesWhenNoDuplicate() {
        when(notificationRepository.existsByUserIdAndMessageAndStatus(
                anyLong(), anyString(), any())).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(
                1L, "Data at 85%", NotificationCategory.USAGE);

        assertThat(result).isNotNull();
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void createNotification_skipsWhenDuplicateExists() {
        when(notificationRepository.existsByUserIdAndMessageAndStatus(
                anyLong(), anyString(), any())).thenReturn(true);

        Notification result = notificationService.createNotification(
                1L, "Data at 85%", NotificationCategory.USAGE);

        assertThat(result).isNull();
        verify(notificationRepository, never()).save(any());
    }

    // ── createNotificationFromRequest ─────────────────────────────────────────

    @Test
    void createNotificationFromRequest_successWithValidCategory() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId(1L);
        req.setMessage("Invoice due");
        req.setCategory("BILLING");

        when(notificationRepository.existsByUserIdAndMessageAndStatus(
                anyLong(), anyString(), any())).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, String> result =
                notificationService.createNotificationFromRequest(req);

        assertThat(result.get("message")).isEqualTo("Notification created successfully");
    }

    @Test
    void createNotificationFromRequest_throwsOnInvalidCategory() {
        NotificationRequest req = new NotificationRequest();
        req.setUserId(1L);
        req.setMessage("Test");
        req.setCategory("INVALID");

        assertThatThrownBy(() -> notificationService.createNotificationFromRequest(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid category");
    }

    // ── fetchAll ──────────────────────────────────────────────────────────────

    @Test
    void fetchAll_returnsCorrectSummary() {
        Notification unread = buildNotification(1L, 1L,
                NotificationCategory.USAGE, NotificationStatus.UNREAD);
        Notification read = buildNotification(2L, 1L,
                NotificationCategory.BILLING, NotificationStatus.READ);

        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(1L))
                .thenReturn(List.of(unread, read));

        NotificationSummaryResponse response = notificationService.fetchAll(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUnreadCount()).isEqualTo(1L);
        assertThat(response.getNotifications()).hasSize(2);
    }

    @Test
    void fetchAll_throwsWhenNoNotifications() {
        when(notificationRepository.findByUserIdOrderByCreatedDateDesc(99L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> notificationService.fetchAll(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No notifications found");
    }

    // ── fetchUnread ───────────────────────────────────────────────────────────

    @Test
    void fetchUnread_returnsUnreadList() {
        Notification n = buildNotification(1L, 1L,
                NotificationCategory.PLAN, NotificationStatus.UNREAD);
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(
                1L, NotificationStatus.UNREAD)).thenReturn(List.of(n));

        Map<String, Object> result = notificationService.fetchUnread(1L);

        assertThat(result.get("unreadCount")).isEqualTo(1);
    }

    @Test
    void fetchUnread_throwsWhenNoneFound() {
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedDateDesc(
                1L, NotificationStatus.UNREAD)).thenReturn(List.of());

        assertThatThrownBy(() -> notificationService.fetchUnread(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_updatesSuccessfully() {
        Notification n = buildNotification(1L, 1L,
                NotificationCategory.USAGE, NotificationStatus.UNREAD);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("READ");

        Map<String, Object> result = notificationService.updateStatus(1L, req);

        assertThat(result.get("newStatus")).isEqualTo("READ");
    }

    @Test
    void updateStatus_throwsOnInvalidStatus() {
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("INVALID");

        assertThatThrownBy(() -> notificationService.updateStatus(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }


    @Test
    void updateStatus_throwsWhenNotificationNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("READ");

        assertThatThrownBy(() -> notificationService.updateStatus(999L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // ── markAllRead ───────────────────────────────────────────────────────────

    @Test
    void markAllRead_marksSuccessfully() {
        when(notificationRepository.countByUserIdAndStatus(
                1L, NotificationStatus.UNREAD)).thenReturn(3L);
        when(notificationRepository.markAllReadForUser(
                1L, NotificationStatus.READ)).thenReturn(3);

        Map<String, Object> result = notificationService.markAllRead(1L);

        assertThat(result.get("updatedCount")).isEqualTo(3);
    }

    @Test
    void markAllRead_throwsWhenNothingToMark() {
        when(notificationRepository.countByUserIdAndStatus(
                1L, NotificationStatus.UNREAD)).thenReturn(0L);

        assertThatThrownBy(() -> notificationService.markAllRead(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteNotification ────────────────────────────────────────────────────

    @Test
    void deleteNotification_deletesSuccessfully() {
        when(notificationRepository.existsById(1L)).thenReturn(true);

        Map<String, String> result = notificationService.deleteNotification(1L);

        assertThat(result.get("message")).contains("deleted successfully");
        verify(notificationRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteNotification_throwsWhenNotFound() {
        when(notificationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.deleteNotification(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
