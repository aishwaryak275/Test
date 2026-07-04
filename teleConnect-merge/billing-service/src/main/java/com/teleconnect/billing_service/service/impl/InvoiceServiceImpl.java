package com.teleconnect.billing_service.service.impl;

import com.teleconnect.billing_service.dto.request.InvoiceGenerationRequest;
import com.teleconnect.billing_service.dto.request.LateFeeRequest;
import com.teleconnect.billing_service.dto.request.LateFeeWaiverRequest;
import com.teleconnect.billing_service.dto.request.PaymentRequest;
import com.teleconnect.billing_service.dto.response.InvoiceResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.entity.Payment;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.enums.PaymentStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.repository.PaymentRepository;
import com.teleconnect.billing_service.service.InvoiceService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, BillingCycleRepository billingCycleRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.billingCycleRepository = billingCycleRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(InvoiceGenerationRequest request) {
        BillingCycle cycle = billingCycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billing cycle not found: " + request.getCycleId()));

        if (cycle.getStatus() == BillingCycleStatus.CLOSED) {
            throw new BillingException("Cannot generate invoice for a closed billing cycle");
        }

        invoiceRepository.findByAccountIdAndCycleId(request.getAccountId(), request.getCycleId())
                .ifPresent(inv -> {
                    throw new BillingException(
                            "Invoice already exists for account " + request.getAccountId()
                            + " and cycle " + request.getCycleId());
                });

        BigDecimal total = request.getPlanCharges()
                .add(request.getExcessCharges())
                .add(request.getAddOnCharges())
                .add(request.getTaxes())
                .setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .accountId(request.getAccountId())
                .cycleId(request.getCycleId())
                .planCharges(request.getPlanCharges())
                .excessCharges(request.getExcessCharges())
                .addOnCharges(request.getAddOnCharges())
                .taxes(request.getTaxes())
                .totalAmount(total)
                .dueDate(cycle.getCycleEnd().plusDays(15))
                .status(InvoiceStatus.GENERATED)
                .build();

        cycle.setStatus(BillingCycleStatus.GENERATED);
        cycle.setGeneratedDate(LocalDate.now());
        billingCycleRepository.save(cycle);

        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        return toResponse(findById(invoiceId));
    }

    @Override
    public List<InvoiceResponse> getInvoicesByAccount(Long accountId) {
        return invoiceRepository.findByAccountId(accountId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<InvoiceResponse> getInvoicesByAccount(Long accountId, InvoiceStatus status,
                                                      LocalDate fromDate, LocalDate toDate) {
        List<Invoice> invoices;
        if (status != null && fromDate != null && toDate != null) {
            invoices = invoiceRepository.findByAccountIdAndStatusAndDueDateBetween(
                    accountId, status, fromDate, toDate);
        } else if (status != null) {
            invoices = invoiceRepository.findByAccountIdAndStatus(accountId, status);
        } else if (fromDate != null && toDate != null) {
            invoices = invoiceRepository.findByAccountIdAndDueDateBetween(accountId, fromDate, toDate);
        } else {
            invoices = invoiceRepository.findByAccountId(accountId);
        }
        return invoices.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceResponse processPayment(PaymentRequest request) {
        Invoice invoice = findById(request.getInvoiceId());

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Invoice is already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            throw new BillingException("Cannot process payment for a disputed invoice. Resolve the dispute first.");
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
        paymentRepository.save(payment);

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAmount(request.getAmountPaid());
        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public InvoiceResponse payInvoice(Long invoiceId, PaymentRequest request) {
        request.setInvoiceId(invoiceId);
        return processPayment(request);
    }

    @Override
    @Transactional
    public InvoiceResponse applyLateFee(Long invoiceId, LateFeeRequest request) {
        Invoice invoice = findById(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BillingException("Late fee can only be applied to OVERDUE invoices");
        }

        BigDecimal feeAmount = request.getFeeAmount().setScale(2, RoundingMode.HALF_UP);
        invoice.setLateFee(invoice.getLateFee().add(feeAmount));
        invoice.setTotalAmount(invoice.getTotalAmount().add(feeAmount));

        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public InvoiceResponse waiveLateFee(Long invoiceId, LateFeeWaiverRequest request) {
        Invoice invoice = findById(invoiceId);

        if (invoice.getLateFee() == null || invoice.getLateFee().compareTo(BigDecimal.ZERO) == 0) {
            throw new BillingException("No late fee to waive for invoice: " + invoiceId);
        }

        invoice.setTotalAmount(invoice.getTotalAmount().subtract(invoice.getLateFee()));
        invoice.setLateFee(BigDecimal.ZERO);

        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void markOverdueInvoices() {
        List<Invoice> generatedOverdue = invoiceRepository
                .findByStatusAndDueDateBefore(InvoiceStatus.GENERATED, LocalDate.now());
        List<Invoice> sentOverdue = invoiceRepository
                .findByStatusAndDueDateBefore(InvoiceStatus.SENT, LocalDate.now());

        generatedOverdue.forEach(inv -> inv.setStatus(InvoiceStatus.OVERDUE));
        sentOverdue.forEach(inv -> inv.setStatus(InvoiceStatus.OVERDUE));

        invoiceRepository.saveAll(generatedOverdue);
        invoiceRepository.saveAll(sentOverdue);
    }

    @Override
    @Transactional
    public InvoiceResponse sendInvoice(Long invoiceId) {
        Invoice invoice = findById(invoiceId);

        if (invoice.getStatus() != InvoiceStatus.GENERATED) {
            throw new BillingException("Only GENERATED invoices can be sent. Current status: " + invoice.getStatus());
        }

        invoice.setStatus(InvoiceStatus.SENT);
        return toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public byte[] downloadInvoicePdf(Long invoiceId) {
        Invoice invoice = findById(invoiceId);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                float margin = 50;
                float yStart = page.getMediaBox().getHeight() - margin;
                float leading = 18;

                cs.beginText();
                cs.setFont(bold, 18);
                cs.newLineAtOffset(margin, yStart);
                cs.showText("TeleConnect - Invoice");
                cs.endText();

                float y = yStart - 30;
                writeRow(cs, bold, regular, margin, y, "Invoice ID", invoice.getInvoiceId().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Account ID", invoice.getAccountId().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Cycle ID", invoice.getCycleId().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Due Date", invoice.getDueDate().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Status", invoice.getStatus().toString());

                y -= 25;
                cs.beginText();
                cs.setFont(bold, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Charge Breakdown");
                cs.endText();

                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Plan Charges", invoice.getPlanCharges().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Excess Charges", invoice.getExcessCharges().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Add-On Charges", invoice.getAddOnCharges().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Taxes", invoice.getTaxes().toString());

                if (invoice.getLateFee() != null && invoice.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                    y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Late Fee", invoice.getLateFee().toString());
                }

                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Total Amount", invoice.getTotalAmount().toString());
                y -= leading;
                writeRow(cs, bold, regular, margin, y, "Amount Paid", invoice.getPaidAmount().toString());

                y -= 25;
                cs.beginText();
                cs.setFont(regular, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Payment instructions: Please pay via UPI/NEFT/CARD before the due date to avoid late fees.");
                cs.endText();

                y -= leading;
                cs.beginText();
                cs.setFont(regular, 9);
                cs.newLineAtOffset(margin, y);
                cs.showText("Generated: " + LocalDateTime.now());
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BillingException("Failed to generate PDF for invoice: " + invoiceId);
        }
    }

    @Override
    public byte[] downloadAccountStatementPdf(Long accountId) {
        List<Invoice> invoices = invoiceRepository.findByAccountId(accountId);
        if (invoices.isEmpty()) {
            throw new ResourceNotFoundException("No invoices found for account: " + accountId);
        }

        try (PDDocument document = new PDDocument()) {
            PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // ── Cover / Summary Page ────────────────────────────────────────────
            PDPage coverPage = new PDPage(PDRectangle.A4);
            document.addPage(coverPage);

            BigDecimal grandTotal   = invoices.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grandPaid    = invoices.stream().map(Invoice::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grandLateFee = invoices.stream().map(Invoice::getLateFee).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal grandBalance = grandTotal.subtract(grandPaid);

            try (PDPageContentStream cs = new PDPageContentStream(document, coverPage)) {
                float margin = 50;
                float y = coverPage.getMediaBox().getHeight() - margin;
                float leading = 20;

                // Title
                cs.beginText();
                cs.setFont(bold, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("TeleConnect - Account Statement");
                cs.endText();

                y -= 10;
                drawLine(cs, margin, y, coverPage.getMediaBox().getWidth() - margin, y);
                y -= 25;

                writeRow(cs, bold, regular, margin, y, "Account ID",       accountId.toString());           y -= leading;
                writeRow(cs, bold, regular, margin, y, "Generated On",     LocalDateTime.now().toString()); y -= leading;
                writeRow(cs, bold, regular, margin, y, "Total Invoices",   String.valueOf(invoices.size())); y -= leading;

                y -= 10;
                drawLine(cs, margin, y, coverPage.getMediaBox().getWidth() - margin, y);
                y -= 20;

                cs.beginText();
                cs.setFont(bold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Summary");
                cs.endText();
                y -= leading;

                writeRow(cs, bold, regular, margin, y, "Grand Total Billed",  grandTotal.setScale(2, RoundingMode.HALF_UP).toString());   y -= leading;
                writeRow(cs, bold, regular, margin, y, "Grand Total Paid",    grandPaid.setScale(2, RoundingMode.HALF_UP).toString());    y -= leading;
                writeRow(cs, bold, regular, margin, y, "Total Late Fees",     grandLateFee.setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                writeRow(cs, bold, regular, margin, y, "Outstanding Balance", grandBalance.setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;

                y -= 20;
                cs.beginText();
                cs.setFont(bold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Invoice List");
                cs.endText();
                y -= leading;

                // Header row
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin,      y); cs.showText("Inv ID");      cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 60,  y); cs.showText("Cycle ID");    cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 120, y); cs.showText("Due Date");    cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 210, y); cs.showText("Total");       cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 290, y); cs.showText("Paid");        cs.endText();
                cs.beginText(); cs.setFont(bold, 10); cs.newLineAtOffset(margin + 360, y); cs.showText("Status");      cs.endText();
                y -= 5;
                drawLine(cs, margin, y, coverPage.getMediaBox().getWidth() - margin, y);
                y -= 15;

                for (Invoice inv : invoices) {
                    if (y < 60) break; // avoid overflow on cover page
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin,      y); cs.showText(inv.getInvoiceId().toString());                          cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 60,  y); cs.showText(inv.getCycleId().toString());                            cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 120, y); cs.showText(inv.getDueDate().toString());                             cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 210, y); cs.showText(inv.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString()); cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 290, y); cs.showText(inv.getPaidAmount().setScale(2, RoundingMode.HALF_UP).toString());  cs.endText();
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin + 360, y); cs.showText(inv.getStatus().toString());                              cs.endText();
                    y -= 15;
                }
            }

            // ── One Detailed Page Per Invoice ───────────────────────────────────
            for (Invoice invoice : invoices) {
                PDPage detailPage = new PDPage(PDRectangle.A4);
                document.addPage(detailPage);

                try (PDPageContentStream cs = new PDPageContentStream(document, detailPage)) {
                    float margin = 50;
                    float y = detailPage.getMediaBox().getHeight() - margin;
                    float leading = 20;

                    cs.beginText();
                    cs.setFont(bold, 16);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Invoice #" + invoice.getInvoiceId() + " — Detail");
                    cs.endText();
                    y -= 10;
                    drawLine(cs, margin, y, detailPage.getMediaBox().getWidth() - margin, y);
                    y -= 25;

                    writeRow(cs, bold, regular, margin, y, "Invoice ID",   invoice.getInvoiceId().toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Account ID",   invoice.getAccountId().toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Cycle ID",     invoice.getCycleId().toString());   y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Due Date",     invoice.getDueDate().toString());   y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Status",       invoice.getStatus().toString());    y -= leading;

                    y -= 10;
                    drawLine(cs, margin, y, detailPage.getMediaBox().getWidth() - margin, y);
                    y -= 20;

                    cs.beginText(); cs.setFont(bold, 13); cs.newLineAtOffset(margin, y); cs.showText("Charge Breakdown"); cs.endText();
                    y -= leading;

                    writeRow(cs, bold, regular, margin, y, "Plan Charges",   invoice.getPlanCharges().setScale(2, RoundingMode.HALF_UP).toString());   y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Excess Charges", invoice.getExcessCharges().setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Add-On Charges", invoice.getAddOnCharges().setScale(2, RoundingMode.HALF_UP).toString());  y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Taxes",          invoice.getTaxes().setScale(2, RoundingMode.HALF_UP).toString());          y -= leading;

                    if (invoice.getLateFee() != null && invoice.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                        writeRow(cs, bold, regular, margin, y, "Late Fee", invoice.getLateFee().setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                    }

                    y -= 5;
                    drawLine(cs, margin, y, detailPage.getMediaBox().getWidth() - margin, y);
                    y -= 20;

                    writeRow(cs, bold, regular, margin, y, "Total Amount",   invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;
                    writeRow(cs, bold, regular, margin, y, "Amount Paid",    invoice.getPaidAmount().setScale(2, RoundingMode.HALF_UP).toString());  y -= leading;

                    BigDecimal balance = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
                    writeRow(cs, bold, regular, margin, y, "Balance Due",    balance.setScale(2, RoundingMode.HALF_UP).toString()); y -= leading;

                    y -= 20;
                    cs.beginText(); cs.setFont(regular, 9); cs.newLineAtOffset(margin, y);
                    cs.showText("Generated: " + LocalDateTime.now());
                    cs.endText();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();

        } catch (IOException e) {
            throw new BillingException("Failed to generate account statement PDF for account: " + accountId);
        }
    }

    private void drawLine(PDPageContentStream cs, float x1, float y, float x2, float y2) throws IOException {
        cs.moveTo(x1, y);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private void writeRow(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                          float x, float y, String label, String value) throws IOException {
        cs.beginText();
        cs.setFont(bold, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(label + ": ");
        cs.endText();
        cs.beginText();
        cs.setFont(regular, 11);
        cs.newLineAtOffset(x + 130, y);
        cs.showText(value);
        cs.endText();
    }

    private Invoice findById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found with ID: " + invoiceId));
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .accountId(invoice.getAccountId())
                .cycleId(invoice.getCycleId())
                .planCharges(invoice.getPlanCharges())
                .excessCharges(invoice.getExcessCharges())
                .addOnCharges(invoice.getAddOnCharges())
                .taxes(invoice.getTaxes())
                .totalAmount(invoice.getTotalAmount())
                .paidAmount(invoice.getPaidAmount())
                .lateFee(invoice.getLateFee())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .build();
    }
}
