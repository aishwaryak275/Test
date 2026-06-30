package com.teleConnect.billing.inovice.controller;

import com.teleConnect.billing.inovice.dto.request.LateFeeRequest;
import com.teleConnect.billing.inovice.dto.request.PayInvoiceRequest;
import com.teleConnect.billing.inovice.dto.request.WaiveLateFeeRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.InvoiceResponse;
import com.teleConnect.billing.inovice.dto.response.MessageResponse;
import com.teleConnect.billing.inovice.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teleConnect/billing/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * GET /teleConnect/billing/invoices/{invoiceId}
     * Returns full invoice details with all charge components.
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    /**
     * GET /teleConnect/billing/invoices/account/{accountId}
     * Returns paginated invoice history for an account.
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoicesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(invoiceService.getInvoicesByAccount(accountId, status, pageable));
    }

    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/pay
     * Records a payment. Partial payments supported.
     */
    @PostMapping("/{invoiceId}/pay")
    public ResponseEntity<MessageResponse> pay(
            @PathVariable Long invoiceId,
            @Valid @RequestBody PayInvoiceRequest request) {
        return ResponseEntity.ok(new MessageResponse(invoiceService.recordPayment(invoiceId, request).getMessage()));
    }

    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/latefee
     * Applies a late fee to an Overdue invoice.
     */
    @PostMapping("/{invoiceId}/latefee")
    public ResponseEntity<MessageResponse> applyLateFee(
            @PathVariable Long invoiceId,
            @Valid @RequestBody LateFeeRequest request) {
        return ResponseEntity.ok(new MessageResponse(invoiceService.applyLateFee(invoiceId, request).getMessage()));
    }

    /**
     * POST /teleConnect/billing/invoices/{invoiceId}/latefee/waive
     * Waives an applied late fee. Every waiver is audit-logged.
     */
    @PostMapping("/{invoiceId}/latefee/waive")
    public ResponseEntity<MessageResponse> waiveLateFee(
            @PathVariable Long invoiceId,
            @Valid @RequestBody WaiveLateFeeRequest request) {
        return ResponseEntity.ok(new MessageResponse(invoiceService.waiveLateFee(invoiceId, request).getMessage()));
    }
}
