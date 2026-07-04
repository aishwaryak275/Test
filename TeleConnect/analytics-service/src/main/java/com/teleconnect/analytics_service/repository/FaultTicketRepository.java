package com.teleconnect.analytics_service.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teleconnect.analytics_service.entity.FaultTicket;
import com.teleconnect.analytics_service.enums.FaultPriority;
import com.teleconnect.analytics_service.enums.FaultStatus;

public interface FaultTicketRepository extends JpaRepository<FaultTicket, Long> {

    List<FaultTicket> findByStatusIn(List<FaultStatus> statuses);

    @Query("SELECT t FROM FaultTicket t WHERE t.status IN ('RESOLVED','CLOSED') " +
            "AND t.raisedDate BETWEEN :start AND :end")
    List<FaultTicket> findClosedInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t FROM FaultTicket t WHERE t.status IN ('OPEN','IN_PROGRESS') " +
            "AND t.raisedDate < :threshold")
    List<FaultTicket> findOpenBreachingBefore(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(t) FROM FaultTicket t WHERE t.accountId = :accountId " +
            "AND t.status IN ('OPEN','IN_PROGRESS') AND t.raisedDate >= :since")
    long countOpenByAccountSince(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);

    List<FaultTicket> findByPriorityAndStatusIn(FaultPriority priority, List<FaultStatus> statuses);

    @Query("SELECT t FROM FaultTicket t WHERE t.raisedDate BETWEEN :start AND :end")
    List<FaultTicket> findByRaisedDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
