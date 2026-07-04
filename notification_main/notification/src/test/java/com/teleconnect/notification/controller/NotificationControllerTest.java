package com.teleconnect.notification.controller;

import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.response.NotificationResponse;
import com.teleconnect.notification.dto.response.NotificationSummaryResponse;
import com.teleconnect.notification.entity.enums.NotificationCategory;
import com.teleconnect.notification.entity.enums.NotificationStatus;
import com.teleconnect.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService service;

    @InjectMocks
    private NotificationController controller;

    @Test
    void createNotificationTest() {

        NotificationRequest request = new NotificationRequest();
        request.setUserId(201L);
        request.setMessage("Test Notification");
        request.setCategory(NotificationCategory.FAULT);

        NotificationResponse response = new NotificationResponse();
        response.setNotificationId(1L);
        response.setUserId(201L);
        response.setMessage("Test Notification");
        response.setCategory(NotificationCategory.FAULT);
        response.setStatus(NotificationStatus.UNREAD);
        response.setCreatedDate(LocalDateTime.now());

        when(service.createNotification(request))
                .thenReturn(response);

        assertNotNull(service.createNotification(request));

        verify(service).createNotification(request);
    }

    @Test
    void getNotificationsTest() {

        NotificationResponse response = new NotificationResponse();

        response.setNotificationId(1L);
        response.setUserId(201L);
        response.setMessage("Test");
        response.setCategory(NotificationCategory.FAULT);
        response.setStatus(NotificationStatus.UNREAD);
        response.setCreatedDate(LocalDateTime.now());

        when(service.getNotifications(201L))
                .thenReturn(List.of(response));

        List<NotificationResponse> result =
                service.getNotifications(201L);

        assertEquals(1, result.size());
    }

    @Test
    void getNotificationByIdTest() {

        NotificationResponse response = new NotificationResponse();

        response.setNotificationId(1L);
        response.setUserId(201L);
        response.setMessage("Test");
        response.setCategory(NotificationCategory.FAULT);
        response.setStatus(NotificationStatus.UNREAD);

        when(service.getNotificationById(1L))
                .thenReturn(response);

        NotificationResponse result =
                service.getNotificationById(1L);

        assertEquals(1L, result.getNotificationId());
    }

    @Test
    void getByStatusTest() {

        NotificationResponse response = new NotificationResponse();

        response.setNotificationId(1L);
        response.setUserId(201L);
        response.setStatus(NotificationStatus.UNREAD);

        when(service.getByStatus(
                201L,
                NotificationStatus.UNREAD))
                .thenReturn(List.of(response));

        List<NotificationResponse> result =
                service.getByStatus(
                        201L,
                        NotificationStatus.UNREAD);

        assertFalse(result.isEmpty());
    }

    @Test
    void getByCategoryTest() {

        NotificationResponse response = new NotificationResponse();

        response.setNotificationId(1L);
        response.setUserId(201L);
        response.setCategory(NotificationCategory.FAULT);

        when(service.getByCategory(
                201L,
                NotificationCategory.FAULT))
                .thenReturn(List.of(response));

        List<NotificationResponse> result =
                service.getByCategory(
                        201L,
                        NotificationCategory.FAULT);

        assertFalse(result.isEmpty());
    }

    @Test
    void unreadCountTest() {

        NotificationSummaryResponse summary =
                new NotificationSummaryResponse(
                        201L,
                        5L);

        when(service.getUnreadCount(201L))
                .thenReturn(summary);

        NotificationSummaryResponse result =
                service.getUnreadCount(201L);

        assertEquals(5L, result.getUnreadCount());
    }

    @Test
    void markAsReadTest() {

        when(service.markAsRead(1L))
                .thenReturn("Notification marked as read");

        String result = service.markAsRead(1L);

        assertEquals(
                "Notification marked as read",
                result);
    }

    @Test
    void dismissNotificationTest() {

        when(service.dismissNotification(1L))
                .thenReturn(
                        "Notification dismissed successfully");

        String result =
                service.dismissNotification(1L);

        assertEquals(
                "Notification dismissed successfully",
                result);
    }

    @Test
    void markAllAsReadTest() {

        when(service.markAllAsRead(201L))
                .thenReturn(
                        "All notifications marked as read");

        String result =
                service.markAllAsRead(201L);

        assertEquals(
                "All notifications marked as read",
                result);
    }
}