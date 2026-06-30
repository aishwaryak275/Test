package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    // Used by generate endpoint: find all Open cycles whose cycleEnd <= given date
    List<BillingCycle> findByStatusAndCycleEndLessThanEqual(CycleStatus status, LocalDate cycleEnd);

    // Used by GET /cycles/{accountId}
    Page<BillingCycle> findByAccountID(Long accountID, Pageable pageable);

    Page<BillingCycle> findByAccountIDAndStatus(Long accountID, CycleStatus status, Pageable pageable);
}
