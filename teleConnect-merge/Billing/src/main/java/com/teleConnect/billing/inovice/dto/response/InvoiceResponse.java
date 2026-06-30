package com.teleConnect.billing.inovice.dto.response;

import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long invoiceID;
    private Long accountID;
    private Long cycleID;
    private BigDecimal planCharges;
    private BigDecimal excessCharges;
    private BigDecimal addOnCharges;
    private BigDecimal taxes;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private InvoiceStatus status;

    public static InvoiceResponse from(Invoice invoice) {
        return InvoiceResponse.builder()
                .invoiceID(invoice.getInvoiceID())
                .accountID(invoice.getAccountID())
                .cycleID(invoice.getCycle().getCycleID())
                .planCharges(invoice.getPlanCharges())
                .excessCharges(invoice.getExcessCharges())
                .addOnCharges(invoice.getAddOnCharges())
                .taxes(invoice.getTaxes())
                .totalAmount(invoice.getTotalAmount())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .build();
    }
}
