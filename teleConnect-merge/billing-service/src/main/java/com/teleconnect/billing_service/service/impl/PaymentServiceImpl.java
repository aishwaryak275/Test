package com.teleconnect.billing_service.service.impl;

import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.PaymentResponse;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.entity.Payment;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.enums.PaymentStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.repository.PaymentRepository;
import com.teleconnect.billing_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public PaymentResponse makePayment(PaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Invoice " + request.getInvoiceId() + " is already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            throw new BillingException(
                    "Cannot process payment for a disputed invoice. Resolve the dispute first.");
        }
        if (request.getAmountPaid().compareTo(invoice.getTotalAmount()) < 0) {
            throw new BillingException(
                    "Payment amount " + request.getAmountPaid()
                    + " is less than the invoice total " + invoice.getTotalAmount());
        }

        if (request.getTransactionRef() != null && !request.getTransactionRef().isBlank()) {
            paymentRepository.findByTransactionRef(request.getTransactionRef())
                    .ifPresent(existing -> {
                        throw new BillingException(
                                "Duplicate transaction reference: " + request.getTransactionRef());
                    });
        }

        Payment payment = Payment.builder()
                .invoiceId(request.getInvoiceId())
                .amountPaid(request.getAmountPaid())
                .paymentMethod(request.getPaymentMethod())
                .transactionRef(request.getTransactionRef())
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment saved = paymentRepository.save(payment);

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(request.getAmountPaid());
        invoiceRepository.save(invoice);

        return toResponse(saved);
    }

    @Override
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with ID: " + paymentId));
        return toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new ResourceNotFoundException("Invoice not found with ID: " + invoiceId);
        }
        return paymentRepository.findByInvoiceId(invoiceId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(payment.getInvoiceId())
                .amountPaid(payment.getAmountPaid())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .transactionRef(payment.getTransactionRef())
                .status(payment.getStatus())
                .build();
    }
}
