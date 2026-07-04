package com.teleconnect.analytics_service.repository;

import com.teleconnect.analytics_service.entity.SubscriberAccount;
import com.teleconnect.analytics_service.enums.AccountStatus;
import com.teleconnect.analytics_service.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriberAccountRepository extends JpaRepository<SubscriberAccount, Long> {

    long countByStatus(AccountStatus status);

    long countByStatusAndRegistrationDateBefore(AccountStatus status, LocalDate date);

    long countByStatusAndRegistrationDateBetween(AccountStatus status, LocalDate start, LocalDate end);

    @Query("SELECT COUNT(a) FROM SubscriberAccount a WHERE a.status = :status " +
            "AND a.registrationDate BETWEEN :start AND :end AND a.accountType = :type")
    long countByStatusAndPeriodAndType(@Param("status") AccountStatus status,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end,
                                       @Param("type") AccountType type);

    List<SubscriberAccount> findByStatusAndRegistrationDateBetween(AccountStatus status, LocalDate start, LocalDate
            end);

    @Query("SELECT a FROM SubscriberAccount a WHERE a.status = 'TERMINATED' " +
            "AND a.registrationDate BETWEEN :start AND :end")
    List<SubscriberAccount> findTerminatedInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<SubscriberAccount> findByRegionId(Long regionId);

    @Query("SELECT COUNT(a) FROM SubscriberAccount a WHERE a.status = 'ACTIVE' AND a.regionId = :regionId")
    long countActiveByRegion(@Param("regionId") Long regionId);
}
