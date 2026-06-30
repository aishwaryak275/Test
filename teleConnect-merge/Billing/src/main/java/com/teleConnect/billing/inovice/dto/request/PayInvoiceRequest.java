package com.teleConnect.billing.inovice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayInvoiceRequest(

        @NotNull(message = "amountPaid is required")
        @DecimalMin(value = "0.01", message = "amountPaid must be greater than 0")
        BigDecimal amountPaid,

        @NotBlank(message = "paymentMethod is required")
        String paymentMethod,

        @NotBlank(message = "transactionRef is required")
        String transactionRef
) {}
