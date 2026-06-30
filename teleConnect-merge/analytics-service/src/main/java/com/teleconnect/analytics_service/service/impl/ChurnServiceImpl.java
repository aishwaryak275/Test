package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.dto.response.ChurnReportResponse;
import com.teleconnect.analytics_service.entity.SIMLine;
import com.teleconnect.analytics_service.entity.SubscriberAccount;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.repository.BillingDisputeRepository;
import com.teleconnect.analytics_service.repository.FaultTicketRepository;
import com.teleconnect.analytics_service.repository.SIMLineRepository;
import com.teleconnect.analytics_service.repository.SubscriberAccountRepository;
import com.teleconnect.analytics_service.service.ChurnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChurnServiceImpl implements ChurnService {

    private static final Logger log = LoggerFactory.getLogger(ChurnServiceImpl.class);

    private final SubscriberAccountRepository subscriberAccountRepository;
    private final SIMLineRepository simLineRepository;
    private final FaultTicketRepository faultTicketRepository;
    private final BillingDisputeRepository billingDisputeRepository;

    @Value("${analytics.churn.threshold:5.0}")
    private double churnThreshold;

    public ChurnServiceImpl(SubscriberAccountRepository subscriberAccountRepository,
                            SIMLineRepository simLineRepository,
                            FaultTicketRepository faultTicketRepository,
                            BillingDisputeRepository billingDisputeRepository) {
        this.subscriberAccountRepository = subscriberAccountRepository;
        this.simLineRepository = simLineRepository;
        this.faultTicketRepository = faultTicketRepository;
        this.billingDisputeRepository = billingDisputeRepository;
    }

    @Override
    public ChurnReportResponse computeChurn(LocalDate periodStart, LocalDate periodEnd, String region) {
        long subscribersAtStart = subscriberAccountRepository
                .countByStatusAndRegistrationDateBefore(AccountStatus.ACTIVE, periodStart);

        List<SubscriberAccount> terminated = subscriberAccountRepository
                .findTerminatedInPeriod(periodStart, periodEnd);

        List<SIMLine> portedOut = simLineRepository.findPortedOutInPeriod(periodStart, periodEnd);

        long terminatedCount = terminated.size();
        long portedOutCount = portedOut.size();
        long grossChurned = terminatedCount + portedOutCount;

        double churnRate = subscribersAtStart > 0
                ? (double) grossChurned / subscribersAtStart * 100
                : 0.0;

        boolean highChurnAlert = churnRate > churnThreshold;
        if (highChurnAlert) {
            log.warn("HIGH CHURN ALERT: Churn rate {}% exceeds threshold {}%", churnRate, churnThreshold);
        }

        List<Long> atRiskIds = identifyAtRiskAccounts(periodStart);

        ChurnReportResponse response = new ChurnReportResponse();
        response.setPeriodStart(periodStart);
        response.setPeriodEnd(periodEnd);
        response.setRegion(region);
        response.setSubscribersAtPeriodStart(subscribersAtStart);
        response.setTerminatedAccounts(terminatedCount);
        response.setPortedOutLines(portedOutCount);
        response.setGrossChurned(grossChurned);
        response.setChurnRate(Math.round(churnRate * 100.0) / 100.0);
        response.setHighChurnAlert(highChurnAlert);
        response.setAtRiskAccountIds(atRiskIds);
        response.setAtRiskCount(atRiskIds.size());

        return response;
    }

    private List<Long> identifyAtRiskAccounts(LocalDate asOf) {
        LocalDateTime twoMonthsAgo = asOf.minusMonths(2).atStartOfDay();
        LocalDate twoMonthsAgoDate = asOf.minusMonths(2);

        List<Long> atRisk = new ArrayList<>();

        subscriberAccountRepository.findAll().stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .forEach(account -> {
                    long openTickets = faultTicketRepository.countOpenByAccountSince(
                            account.getAccountId(), twoMonthsAgo);
                    long disputes = billingDisputeRepository.countBySubscriberSince(
                            account.getSubscriberId(), twoMonthsAgoDate);

                    if (openTickets >= 2 || disputes >= 1) {
                        atRisk.add(account.getAccountId());
                    }
                });

        return atRisk;
    }
}
