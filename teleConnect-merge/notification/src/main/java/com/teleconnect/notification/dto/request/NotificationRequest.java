package com.teleconnect.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "message is required")
    private String message;

    @NotBlank(message = "category is required — USAGE / BILLING / FAULT / PLAN / COMPLIANCE")
    private String category;
}
