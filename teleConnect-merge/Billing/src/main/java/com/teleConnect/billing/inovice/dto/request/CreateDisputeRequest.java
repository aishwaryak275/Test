package com.teleConnect.billing.inovice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateDisputeRequest(

        @NotNull(message = "invoiceId is required")
        Long invoiceId,

        @NotBlank(message = "disputeReason is required")
        String disputeReason,

        @NotNull(message = "disputedAmount is required")
        @DecimalMin(value = "0.01", message = "disputedAmount must be greater than 0")
        BigDecimal disputedAmount,

        String description
) {}
