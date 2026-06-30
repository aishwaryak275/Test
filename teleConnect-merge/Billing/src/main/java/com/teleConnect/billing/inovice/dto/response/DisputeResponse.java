package com.teleConnect.billing.inovice.dto.response;

import com.teleConnect.billing.inovice.entity.BillingDispute;
import com.teleConnect.billing.inovice.entity.enums.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {

    private Long disputeID;
    private Long invoiceID;
    private Long subscriberID;
    private String disputeReason;
    private BigDecimal disputedAmount;
    private String description;
    private LocalDateTime raisedDate;
    private DisputeStatus status;

    public static DisputeResponse from(BillingDispute dispute) {
        return DisputeResponse.builder()
                .disputeID(dispute.getDisputeID())
                .invoiceID(dispute.getInvoice().getInvoiceID())
                .subscriberID(dispute.getSubscriberID())
                .disputeReason(dispute.getDisputeReason())
                .disputedAmount(dispute.getDisputedAmount())
                .description(dispute.getDescription())
                .raisedDate(dispute.getRaisedDate())
                .status(dispute.getStatus())
                .build();
    }
}
