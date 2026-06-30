package com.teleconnect.analytics_service.repository;

import com.teleconnect.analytics_service.entity.BillingDispute;
import com.teleconnect.analytics_service.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillingDisputeRepository extends JpaRepository<BillingDispute, Long> {

    long countByStatus(DisputeStatus status);

    @Query("SELECT d FROM BillingDispute d WHERE d.raisedDate BETWEEN :start AND :end")
    List<BillingDispute> findByRaisedDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(d) FROM BillingDispute d WHERE d.subscriberId = :subscriberId " +
            "AND d.raisedDate >= :since")
    long countBySubscriberSince(@Param("subscriberId") Long subscriberId, @Param("since") LocalDate since);

    List<BillingDispute> findBySubscriberId(Long subscriberId);
}
