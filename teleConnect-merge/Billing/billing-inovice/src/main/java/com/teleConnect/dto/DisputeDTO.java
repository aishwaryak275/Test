package com.teleConnect.dto;

import java.math.BigDecimal;

public class DisputeDTO {
    public Long invoiceId;
    public Long subscriberId;
    public BigDecimal disputedAmount;
    public String reason;
}
