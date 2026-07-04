package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.dto.response.NetworkUtilisationResponse;
import com.teleconnect.analytics_service.entity.UsageSummary;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.repository.SubscriberAccountRepository;
import com.teleconnect.analytics_service.repository.UsageSummaryRepository;
import com.teleconnect.analytics_service.service.NetworkUtilisationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NetworkUtilisationServiceImpl implements NetworkUtilisationService {

    private final UsageSummaryRepository usageSummaryRepository;
    private final SubscriberAccountRepository subscriberAccountRepository;

    public NetworkUtilisationServiceImpl(UsageSummaryRepository usageSummaryRepository,
                                         SubscriberAccountRepository subscriberAccountRepository) {
        this.usageSummaryRepository = usageSummaryRepository;
        this.subscriberAccountRepository = subscriberAccountRepository;
    }

    @Override
    public NetworkUtilisationResponse computeUtilisation(Long cycleId, String region) {
        List<UsageSummary> summaries;
        long subscriberCount;

        if (region != null && !region.isBlank()) {
            try {
                Long regionId = Long.parseLong(region);
                summaries = usageSummaryRepository.findByCycleAndRegion(cycleId, regionId);
                subscriberCount = subscriberAccountRepository.countActiveByRegion(regionId);
            } catch (NumberFormatException e) {
                summaries = usageSummaryRepository.findByBillingCycleId(cycleId);
                subscriberCount = subscriberAccountRepository.countByStatus(AccountStatus.ACTIVE);
            }
        } else {
            summaries = usageSummaryRepository.findByBillingCycleId(cycleId);
            subscriberCount = subscriberAccountRepository.countByStatus(AccountStatus.ACTIVE);
        }

        long totalData = summaries.stream().mapToLong(UsageSummary::getDataUsedMb).sum();
        long totalVoice = summaries.stream().mapToLong(UsageSummary::getVoiceUsedMin).sum();
        long totalSms = summaries.stream().mapToLong(UsageSummary::getSmsUsed).sum();

        NetworkUtilisationResponse response = new NetworkUtilisationResponse();
        response.setCycleId(cycleId);
        response.setRegion(region);
        response.setTotalDataUsedMb(totalData);
        response.setTotalVoiceUsedMin(totalVoice);
        response.setTotalSmsUsed(totalSms);
        response.setSubscriberCount(subscriberCount);
        response.setAvgDataPerSubscriberMb(subscriberCount > 0 ? (double) totalData / subscriberCount : 0);
        response.setAvgVoicePerSubscriberMin(subscriberCount > 0 ? (double) totalVoice / subscriberCount : 0);

        return response;
    }
}
