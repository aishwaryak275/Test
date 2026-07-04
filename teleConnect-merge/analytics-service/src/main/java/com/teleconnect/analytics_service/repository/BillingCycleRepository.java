package com.teleconnect.analytics_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.teleconnect.analytics_service.entity.BillingCycle;
import com.teleconnect.analytics_service.enums.BillingCycleStatus;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    List<BillingCycle> findByStatus(BillingCycleStatus status);

    Optional<BillingCycle> findTopByStatusOrderByCycleEndDesc(BillingCycleStatus status);

    List<BillingCycle> findByAccountId(Long accountId);
}
