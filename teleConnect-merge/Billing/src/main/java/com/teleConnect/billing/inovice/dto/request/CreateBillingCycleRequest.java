package com.teleConnect.billing.inovice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record CreateBillingCycleRequest(

        @NotNull(message = "accountID is required")
        Long accountID,

        @NotNull(message = "cycleStart is required")
        LocalDate cycleStart,

        @NotNull(message = "cycleEnd is required")
        LocalDate cycleEnd
) {}
