package com.teleconnect.iam.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class AuditLogFilterDTO {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from; // ?from=2024-01-01T00:00:00

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;   // ?to=2024-12-31T23:59:59

    private String action;      // ?action=USER_LOGIN
    private String module;      // ?module=IAM
}
