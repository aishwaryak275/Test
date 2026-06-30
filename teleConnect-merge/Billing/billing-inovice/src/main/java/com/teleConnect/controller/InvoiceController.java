package com.teleConnect.controller;

import com.teleConnect.dto.LateFeeDTO;
import com.teleConnect.dto.PaymentDTO;
import com.teleConnect.entity.Invoice;
import com.teleConnect.entity.Payment;
import com.teleConnect.service.InvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/teleConnect/billing/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/{invoiceId}/pay")
    public ResponseEntity<?> pay(@PathVariable Long invoiceId, @RequestBody PaymentDTO dto) {
        Payment p = invoiceService.payInvoice(invoiceId, dto);
        return ResponseEntity.ok(p);
    }

    @PostMapping("/{invoiceId}/latefee")
    public ResponseEntity<?> applyLateFee(@PathVariable Long invoiceId, @RequestBody LateFeeDTO dto) {
        Invoice inv = invoiceService.applyLateFee(invoiceId, dto.feeAmount == null ? BigDecimal.ZERO : dto.feeAmount);
        return ResponseEntity.ok(inv);
    }

    @PostMapping("/{invoiceId}/latefee/waive")
    public ResponseEntity<?> waiveLateFee(@PathVariable Long invoiceId) {
        Invoice inv = invoiceService.waiveLateFee(invoiceId);
        return ResponseEntity.ok(inv);
    }
}
