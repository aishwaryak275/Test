package com.teleconnect.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class NotificationSummaryResponse {
    private Long userId;
    private long unreadCount;
    private List<NotificationResponse> notifications;
}
