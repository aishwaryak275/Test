package com.teleconnect.notification.controller;

import com.teleconnect.notification.dto.request.NotificationRequest;
import com.teleconnect.notification.dto.request.StatusUpdateRequest;
import com.teleconnect.notification.dto.response.NotificationResponse;
import com.teleconnect.notification.dto.response.NotificationSummaryResponse;
import com.teleconnect.notification.exception.GlobalExceptionHandler;
import com.teleconnect.notification.exception.ResourceNotFoundException;
import com.teleconnect.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private NotificationResponse buildResponse() {
        NotificationResponse r = new NotificationResponse();
        r.setNotificationId(1L);
        r.setUserId(1L);
        r.setMessage("Data usage at 85%");
        r.setCategory("USAGE");
        r.setStatus("UNREAD");
        r.setCreatedDate(LocalDateTime.now());
        return r;
    }

    // ── POST /create ──────────────────────────────────────────────────────────

    @Test
    void createNotification_returns201() throws Exception {
        when(notificationService.createNotificationFromRequest(any()))
                .thenReturn(Map.of("message", "Notification created successfully"));

        NotificationRequest req = new NotificationRequest();
        req.setUserId(1L);
        req.setMessage("Data usage at 85%");
        req.setCategory("USAGE");

        mockMvc.perform(post("/teleConnect/notifications/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Notification created successfully"));
    }

    @Test
    void createNotification_returns400WhenMissingFields() throws Exception {
        NotificationRequest req = new NotificationRequest();
        req.setUserId(1L);

        mockMvc.perform(post("/teleConnect/notifications/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /fetchAll ─────────────────────────────────────────────────────────

    @Test
    void fetchAll_returns200WithSummary() throws Exception {
        NotificationSummaryResponse summary = new NotificationSummaryResponse(
                1L, 1L, List.of(buildResponse()));
        when(notificationService.fetchAll(1L)).thenReturn(summary);

        mockMvc.perform(get("/teleConnect/notifications/fetchAll/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications").isArray());
    }

    @Test
    void fetchAll_returns404WhenNotFound() throws Exception {
        when(notificationService.fetchAll(99L))
                .thenThrow(new ResourceNotFoundException(
                        "No notifications found for userId: 99"));

        mockMvc.perform(get("/teleConnect/notifications/fetchAll/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No notifications found for userId: 99"));
    }

    // ── GET /unreadCount ──────────────────────────────────────────────────────

    @Test
    void getUnreadCount_returns200() throws Exception {
        when(notificationService.getUnreadCount(1L))
                .thenReturn(Map.of("userId", 1L, "unreadCount", 3L));

        mockMvc.perform(get("/teleConnect/notifications/unreadCount/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    // ── PATCH /updateStatus ───────────────────────────────────────────────────

    @Test
    void updateStatus_returns200() throws Exception {
        when(notificationService.updateStatus(anyLong(), any()))
                .thenReturn(Map.of(
                        "message", "Notification status updated successfully",
                        "notificationId", 1L,
                        "newStatus", "READ"));

        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus("READ");

        mockMvc.perform(patch("/teleConnect/notifications/updateStatus/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("READ"));
    }

    // ── PATCH /markAllRead ────────────────────────────────────────────────────

    @Test
    void markAllRead_returns200() throws Exception {
        when(notificationService.markAllRead(1L))
                .thenReturn(Map.of(
                        "message", "All notifications marked as read",
                        "userId", 1L,
                        "updatedCount", 5));

        mockMvc.perform(patch("/teleConnect/notifications/markAllRead/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(5));
    }

    // ── DELETE /delete ────────────────────────────────────────────────────────

    @Test
    void deleteNotification_returns200() throws Exception {
        when(notificationService.deleteNotification(1L))
                .thenReturn(Map.of("message", "Notification deleted successfully"));

        mockMvc.perform(delete("/teleConnect/notifications/delete/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification deleted successfully"));
    }

    @Test
    void deleteNotification_returns404WhenNotFound() throws Exception {
        when(notificationService.deleteNotification(999L))
                .thenThrow(new ResourceNotFoundException(
                        "Notification not found for id: 999"));

        mockMvc.perform(delete("/teleConnect/notifications/delete/999"))
                .andExpect(status().isNotFound());
    }
}
