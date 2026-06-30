package com.teleConnect.billing.inovice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WaiveLateFeeRequest(

        @NotBlank(message = "waiverReason is required")
        String waiverReason,

        @NotBlank(message = "authorisedBy is required")
        String authorisedBy
) {}
