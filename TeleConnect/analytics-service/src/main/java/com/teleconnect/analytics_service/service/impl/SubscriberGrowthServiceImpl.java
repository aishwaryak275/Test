package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.dto.response.SubscriberGrowthResponse;
import com.teleconnect.analytics_service.entity.SIMLine;
import com.teleconnect.analytics_service.entity.SubscriberAccount;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;
import com.teleconnect.analytics_service.repository.SIMLineRepository;
import com.teleconnect.analytics_service.repository.SubscriberAccountRepository;
import com.teleconnect.analytics_service.service.SubscriberGrowthService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriberGrowthServiceImpl implements SubscriberGrowthService {

    private final SubscriberAccountRepository subscriberAccountRepository;
    private final SIMLineRepository simLineRepository;

    public SubscriberGrowthServiceImpl(SubscriberAccountRepository subscriberAccountRepository,
                                       SIMLineRepository simLineRepository) {
        this.subscriberAccountRepository = subscriberAccountRepository;
        this.simLineRepository = simLineRepository;
    }

    @Override
    public SubscriberGrowthResponse computeGrowth(LocalDate periodStart, LocalDate periodEnd) {
        List<SubscriberAccount> newAccounts = subscriberAccountRepository
                .findByStatusAndRegistrationDateBetween(AccountStatus.ACTIVE, periodStart, periodEnd);

        List<SubscriberAccount> terminated = subscriberAccountRepository
                .findTerminatedInPeriod(periodStart, periodEnd);

        List<SIMLine> activatedLines = simLineRepository.findActivatedInPeriod(periodStart, periodEnd);

        long grossAdds = newAccounts.size();
        long terminations = terminated.size();
        long netAdds = grossAdds - terminations;

        long prepaidAdds = newAccounts.stream()
                .filter(a -> a.getAccountType() == AccountType.PREPAID).count();
        long postpaidAdds = newAccounts.stream()
                .filter(a -> a.getAccountType() == AccountType.POSTPAID).count();
        long enterpriseAdds = newAccounts.stream()
                .filter(a -> a.getAccountType() == AccountType.ENTERPRISE).count();

        SubscriberGrowthResponse response = new SubscriberGrowthResponse();
        response.setPeriodStart(periodStart);
        response.setPeriodEnd(periodEnd);
        response.setGrossAdds(grossAdds);
        response.setTerminations(terminations);
        response.setNetAdds(netAdds);
        response.setActiveSIMActivations(activatedLines.size());
        response.setPrepaidAdds(prepaidAdds);
        response.setPostpaidAdds(postpaidAdds);
        response.setEnterpriseAdds(enterpriseAdds);

        return response;
    }
}
