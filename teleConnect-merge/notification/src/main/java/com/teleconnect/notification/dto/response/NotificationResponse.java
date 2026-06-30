package com.teleconnect.notification.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long notificationId;
    private Long userId;
    private String message;
    private String category;
    private String status;
    private LocalDateTime createdDate;
}
