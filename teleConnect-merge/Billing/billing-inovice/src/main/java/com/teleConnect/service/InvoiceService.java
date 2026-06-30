package com.teleConnect.service;

import com.teleConnect.dto.PaymentDTO;
import com.teleConnect.entity.Invoice;
import com.teleConnect.entity.Payment;
import com.teleConnect.repository.InvoiceRepository;
import com.teleConnect.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment payInvoice(Long invoiceId, PaymentDTO dto) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        Payment p = new Payment();
        p.setInvoiceID(invoiceId);
        p.setAmountPaid(dto.amountPaid);
        p.setPaymentDate(LocalDateTime.now());
        p.setPaymentMethod(dto.paymentMethod);
        p.setTransactionRef(dto.transactionRef);
        p.setStatus("COMPLETED");

        paymentRepository.save(p);

        // simplistic: mark invoice PAID if payment >= total + late fee
        if (dto.amountPaid != null) {
            java.math.BigDecimal required = invoice.getTotalAmount().add(invoice.getLateFee());
            if (dto.amountPaid.compareTo(required) >= 0) {
                invoice.setStatus("PAID");
            } else {
                invoice.setStatus("PARTIALLY_PAID");
            }
            invoiceRepository.save(invoice);
        }

        return p;
    }

    @Transactional
    public Invoice applyLateFee(Long invoiceId, java.math.BigDecimal fee) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        invoice.setLateFee(invoice.getLateFee().add(fee));
        invoice.setTotalAmount(invoice.getTotalAmount().add(fee));
        invoiceRepository.save(invoice);
        return invoice;
    }

    @Transactional
    public Invoice waiveLateFee(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        invoice.setTotalAmount(invoice.getTotalAmount().subtract(invoice.getLateFee()));
        invoice.setLateFee(java.math.BigDecimal.ZERO);
        invoice.setLateFeeWaived(true);
        invoiceRepository.save(invoice);
        return invoice;
    }
}
