package com.teleconnect.analytics_service.repository;

import com.teleconnect.analytics_service.entity.UsageSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UsageSummaryRepository extends JpaRepository<UsageSummary, Long> {

    List<UsageSummary> findByBillingCycleId(Long billingCycleId);

    List<UsageSummary> findByLineId(Long lineId);

    @Query("SELECT SUM(u.dataUsedMb) FROM UsageSummary u WHERE u.billingCycleId = :cycleId")
    Long sumDataUsedByCycle(@Param("cycleId") Long cycleId);

    @Query("SELECT SUM(u.voiceUsedMin) FROM UsageSummary u WHERE u.billingCycleId = :cycleId")
    Long sumVoiceUsedByCycle(@Param("cycleId") Long cycleId);

    @Query("SELECT SUM(u.smsUsed) FROM UsageSummary u WHERE u.billingCycleId = :cycleId")
    Long sumSmsUsedByCycle(@Param("cycleId") Long cycleId);

    @Query("SELECT u FROM UsageSummary u WHERE u.billingCycleId = :cycleId AND u.lineId IN " +
            "(SELECT s.lineId FROM SIMLine s WHERE s.accountId IN " +
            "(SELECT a.accountId FROM SubscriberAccount a WHERE a.regionId = :regionId))")
    List<UsageSummary> findByCycleAndRegion(@Param("cycleId") Long cycleId, @Param("regionId") Long regionId);
}
