package com.teleConnect.billing.inovice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LateFeeRequest(

        @NotNull(message = "feeAmount is required")
        @DecimalMin(value = "0.01", message = "feeAmount must be greater than 0")
        BigDecimal feeAmount,

        @NotBlank(message = "reason is required")
        String reason
) {}
