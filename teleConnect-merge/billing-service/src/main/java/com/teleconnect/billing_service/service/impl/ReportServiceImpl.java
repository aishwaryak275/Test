package com.teleconnect.billing_service.service.impl;

import com.teleconnect.billing_service.dto.response.CollectionReportResponse;
import com.teleconnect.billing_service.dto.response.DisputeSummaryResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse.OverdueInvoiceItem;
import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.repository.BillingDisputeRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private BillingDisputeRepository disputeRepository;

    @Override
    public OverdueReportResponse getOverdueReport(String region, String agingBucket) {
        List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);
        LocalDate today = LocalDate.now();

        List<Invoice> filtered = overdueInvoices.stream()
                .filter(inv -> matchesAgingBucket(inv.getDueDate(), today, agingBucket))
                .collect(Collectors.toList());

        BigDecimal totalAmount = filtered.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OverdueInvoiceItem> items = filtered.stream()
                .map(inv -> new OverdueInvoiceItem(
                        inv.getInvoiceId(),
                        inv.getAccountId(),
                        inv.getTotalAmount(),
                        inv.getDueDate(),
                        ChronoUnit.DAYS.between(inv.getDueDate(), today)
                ))
                .collect(Collectors.toList());

        return new OverdueReportResponse(agingBucket, region, filtered.size(), totalAmount, items);
    }

    @Override
    public CollectionReportResponse getCollectionReport(LocalDate fromDate, LocalDate toDate, String region) {
        List<Invoice> invoicesInRange = invoiceRepository.findByDueDateBetween(fromDate, toDate);

        BigDecimal totalBilled = invoicesInRange.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Invoice> paidInvoices = invoicesInRange.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .collect(Collectors.toList());

        BigDecimal totalCollected = paidInvoices.stream()
                .map(inv -> inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = totalBilled.subtract(totalCollected);

        double efficiency = totalBilled.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : totalCollected.divide(totalBilled, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        long invoicesOverdue = invoicesInRange.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                .count();

        return new CollectionReportResponse(
                fromDate, toDate, region,
                totalBilled, totalCollected, totalOutstanding,
                efficiency, paidInvoices.size(), (int) invoicesOverdue);
    }

    @Override
    public DisputeSummaryResponse getDisputeSummary(LocalDate fromDate, LocalDate toDate) {
        List<BillingDispute> disputes = disputeRepository.findByRaisedDateBetween(fromDate, toDate);

        int total = disputes.size();

        long acknowledgedWithin24h = disputes.stream()
                .filter(d -> d.getAcknowledgedDate() != null)
                .filter(d -> !d.getAcknowledgedDate().isAfter(
                        d.getRaisedDate().atStartOfDay().plusHours(24)))
                .count();

        long resolvedWithin5Days = disputes.stream()
                .filter(d -> d.getResolvedDate() != null
                        && d.getStatus() == DisputeStatus.RESOLVED)
                .filter(d -> !d.getResolvedDate().toLocalDate().isAfter(
                        d.getRaisedDate().plusDays(5)))
                .count();

        long slaBreaches = disputes.stream()
                .filter(d -> d.getAcknowledgedDate() == null
                        || d.getAcknowledgedDate().isAfter(
                                d.getRaisedDate().atStartOfDay().plusHours(24)))
                .count();

        double slaComplianceRate = total == 0 ? 0.0
                : BigDecimal.valueOf(acknowledgedWithin24h)
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        long resolved = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.RESOLVED).count();
        long rejected = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.REJECTED).count();
        long open = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.OPEN).count();
        long underReview = disputes.stream().filter(d -> d.getStatus() == DisputeStatus.UNDER_REVIEW).count();

        return new DisputeSummaryResponse(
                fromDate, toDate, total,
                (int) acknowledgedWithin24h, (int) resolvedWithin5Days, (int) slaBreaches,
                slaComplianceRate, (int) resolved, (int) rejected, (int) open, (int) underReview);
    }

    private boolean matchesAgingBucket(LocalDate dueDate, LocalDate today, String agingBucket) {
        if (agingBucket == null || agingBucket.isBlank()) return true;
        long daysOverdue = ChronoUnit.DAYS.between(dueDate, today);
        return switch (agingBucket) {
            case "0-30" -> daysOverdue >= 0 && daysOverdue <= 30;
            case "31-60" -> daysOverdue >= 31 && daysOverdue <= 60;
            case "61-90" -> daysOverdue >= 61 && daysOverdue <= 90;
            case "90+" -> daysOverdue > 90;
            default -> true;
        };
    }
}
