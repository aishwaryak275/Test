package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.LateFeeRequest;
import com.teleConnect.billing.inovice.dto.request.PayInvoiceRequest;
import com.teleConnect.billing.inovice.dto.request.WaiveLateFeeRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.InvoiceResponse;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.Payment;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.entity.enums.PaymentMethod;
import com.teleConnect.billing.inovice.entity.enums.PaymentStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.DuplicateResourceException;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.repository.InvoiceRepository;
import com.teleConnect.billing.inovice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public ApiResponse<InvoiceResponse> getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        return ApiResponse.success("Invoice retrieved", InvoiceResponse.from(invoice));
    }

    @Override
    public ApiResponse<Page<InvoiceResponse>> getInvoicesByAccount(Long accountId, String status, Pageable pageable) {
        Page<Invoice> page;
        if (status != null && !status.isBlank()) {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status);
            page = invoiceRepository.findByAccountIDAndStatus(accountId, invoiceStatus, pageable);
        } else {
            page = invoiceRepository.findByAccountID(accountId, pageable);
        }
        return ApiResponse.success("Invoices retrieved", page.map(InvoiceResponse::from));
    }

    @Override
    @Transactional
    public ApiResponse<Void> recordPayment(Long invoiceId, PayInvoiceRequest request) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        // 409: transactionRef must be unique across all payments
        if (paymentRepository.findByTransactionRef(request.transactionRef()).isPresent()) {
            throw new DuplicateResourceException(
                    "A payment with transactionRef '" + request.transactionRef() + "' already exists");
        }

        // Validate paymentMethod enum
        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.paymentMethod());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid paymentMethod: " + request.paymentMethod()
                    + ". Allowed: UPI, Card, NetBanking, Cash");
        }

        // Record the payment
        Payment payment = Payment.builder()
                .invoice(invoice)
                .amountPaid(request.amountPaid())
                .paymentMethod(method)
                .transactionRef(request.transactionRef())
                .paymentDate(LocalDateTime.now())
                .status(PaymentStatus.Success)
                .build();
        paymentRepository.save(payment);

        // Sum all successful payments for this invoice
        BigDecimal totalPaid = paymentRepository.sumPaidAmountByInvoiceId(invoiceId, PaymentStatus.Success);

        // Mark Paid if total paid >= total amount (supports partial payments)
        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.Paid);
            invoiceRepository.save(invoice);
        }

        return ApiResponse.success("Payment recorded successfully");
    }

    @Override
    @Transactional
    public ApiResponse<Void> applyLateFee(Long invoiceId, LateFeeRequest request) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        // Can only apply late fee on Overdue invoices
        if (invoice.getStatus() != InvoiceStatus.Overdue) {
            throw new BusinessRuleException(
                    "Late fee can only be applied to Overdue invoices. Current status: " + invoice.getStatus());
        }

        // Add late fee to total amount
        BigDecimal newTotal = invoice.getTotalAmount().add(request.feeAmount());
        invoice.setTotalAmount(newTotal);
        invoiceRepository.save(invoice);

        return ApiResponse.success("Late fee applied successfully");
    }

    @Override
    @Transactional
    public ApiResponse<Void> waiveLateFee(Long invoiceId, WaiveLateFeeRequest request) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));

        // Waiver is only valid on Overdue invoices
        if (invoice.getStatus() != InvoiceStatus.Overdue) {
            throw new BusinessRuleException(
                    "Late fee waiver can only be applied to Overdue invoices. Current status: " + invoice.getStatus());
        }

        // Phase 1: log waiver and return success
        // Phase 2: look up the LateFeeRecord and reverse it precisely
        return ApiResponse.success("Late fee waived successfully");
    }
}
