package com.teleconnect.analytics_service.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleconnect.analytics_service.entity.BillingDispute;
import com.teleconnect.analytics_service.enums.DisputeStatus;

public interface BillingDisputeRepository extends JpaRepository<BillingDispute, Long> {

    long countByStatus(DisputeStatus status);

    @Query("SELECT d FROM BillingDispute d WHERE d.raisedDate BETWEEN :start AND :end")
    List<BillingDispute> findByRaisedDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(d) FROM BillingDispute d WHERE d.subscriberId = :subscriberId " +
            "AND d.raisedDate >= :since")
    long countBySubscriberSince(@Param("subscriberId") Long subscriberId, @Param("since") LocalDate since);

    List<BillingDispute> findBySubscriberId(Long subscriberId);
}
