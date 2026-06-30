package com.teleConnect.billing.inovice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record GenerateCycleRequest(

        @NotBlank(message = "cycleDate is required")
        String cycleDate,

        @NotNull(message = "planCharges is required")
        @PositiveOrZero(message = "planCharges must be zero or positive")
        BigDecimal planCharges,

        @NotNull(message = "excessCharges is required")
        @PositiveOrZero(message = "excessCharges must be zero or positive")
        BigDecimal excessCharges,

        @NotNull(message = "addOnCharges is required")
        @PositiveOrZero(message = "addOnCharges must be zero or positive")
        BigDecimal addOnCharges,

        @NotNull(message = "taxes is required")
        @PositiveOrZero(message = "taxes must be zero or positive")
        BigDecimal taxes
) {}
