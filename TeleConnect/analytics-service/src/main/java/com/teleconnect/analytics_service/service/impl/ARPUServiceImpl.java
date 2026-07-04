package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.dto.response.ARPUReportResponse;
import com.teleconnect.analytics_service.entity.Invoice;
import com.teleconnect.analytics_service.entity.SubscriberAccount;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;
import com.teleconnect.analytics_service.enums.InvoiceStatus;
import com.teleconnect.analytics_service.exception.AnalyticsException;
import com.teleconnect.analytics_service.repository.InvoiceRepository;
import com.teleconnect.analytics_service.repository.SubscriberAccountRepository;
import com.teleconnect.analytics_service.service.ARPUService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ARPUServiceImpl implements ARPUService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriberAccountRepository subscriberAccountRepository;

    @Value("${analytics.arpu.decline.threshold:10.0}")
    private double arpuDeclineThreshold;

    public ARPUServiceImpl(InvoiceRepository invoiceRepository,
                           SubscriberAccountRepository subscriberAccountRepository) {
        this.invoiceRepository = invoiceRepository;
        this.subscriberAccountRepository = subscriberAccountRepository;
    }

    @Override
    public ARPUReportResponse computeARPU(Long cycleId, String scope, String scopeValue) {
        if (cycleId == null) {
            throw new AnalyticsException("cycleId is required for ARPU computation");
        }

        List<InvoiceStatus> billableStatuses = List.of(
                InvoiceStatus.PAID, InvoiceStatus.OVERDUE, InvoiceStatus.SENT);

        List<Invoice> invoices = invoiceRepository.findByCycleIdAndStatusIn(cycleId, billableStatuses);

        BigDecimal totalRevenue = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeSubscribers = subscriberAccountRepository.countByStatus(AccountStatus.ACTIVE);

        BigDecimal arpuOverall = activeSubscribers > 0
                ? totalRevenue.divide(BigDecimal.valueOf(activeSubscribers), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal arpuPrepaid = computeARPUForType(invoices, AccountType.PREPAID);
        BigDecimal arpuPostpaid = computeARPUForType(invoices, AccountType.POSTPAID);
        BigDecimal arpuEnterprise = computeARPUForType(invoices, AccountType.ENTERPRISE);

        Map<String, BigDecimal> arpuByRegion = computeARPUByRegion(invoices);

        ARPUReportResponse response = new ARPUReportResponse();
        response.setCycleId(cycleId);
        response.setScope(scope);
        response.setScopeValue(scopeValue);
        response.setTotalRevenue(totalRevenue);
        response.setActiveSubscribers(activeSubscribers);
        response.setArpuOverall(arpuOverall);
        response.setArpuPrepaid(arpuPrepaid);
        response.setArpuPostpaid(arpuPostpaid);
        response.setArpuEnterprise(arpuEnterprise);
        response.setArpuByRegion(arpuByRegion);

        return response;
    }

    private BigDecimal computeARPUForType(List<Invoice> invoices, AccountType type) {
        List<Long> accountIds = subscriberAccountRepository.findAll().stream()
                .filter(a -> a.getAccountType() == type && a.getStatus() == AccountStatus.ACTIVE)
                .map(SubscriberAccount::getAccountId)
                .toList();

        BigDecimal revenue = invoices.stream()
                .filter(i -> accountIds.contains(i.getAccountId()))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = accountIds.size();
        return count > 0
                ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> computeARPUByRegion(List<Invoice> invoices) {
        Map<Long, List<Long>> regionAccounts = new HashMap<>();
        subscriberAccountRepository.findAll().stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE && a.getRegionId() != null)
                .forEach(a -> regionAccounts
                        .computeIfAbsent(a.getRegionId(), k -> new java.util.ArrayList<>())
                        .add(a.getAccountId()));

        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : regionAccounts.entrySet()) {
            BigDecimal revenue = invoices.stream()
                    .filter(i -> entry.getValue().contains(i.getAccountId()))
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long count = entry.getValue().size();
            BigDecimal arpu = count > 0
                    ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            result.put("REGION_" + entry.getKey(), arpu);
        }
        return result;
    }
}
