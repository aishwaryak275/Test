package com.teleconnect.analytics_service.repository;

import com.teleconnect.analytics_service.entity.SIMLine;
import com.teleconnect.analytics_service.enums.SIMStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SIMLineRepository extends JpaRepository<SIMLine, Long> {

    List<SIMLine> findByAccountId(Long accountId);

    long countByStatus(SIMStatus status);

    @Query("SELECT s FROM SIMLine s WHERE s.status = 'PORTED_OUT' " +
            "AND s.activationDate BETWEEN :start AND :end")
    List<SIMLine> findPortedOutInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT s FROM SIMLine s WHERE s.activationDate BETWEEN :start AND :end")
    List<SIMLine> findActivatedInPeriod(@Param("start") LocalDate start, @Param("end") LocalDate end);

    long countByAccountIdAndStatus(Long accountId, SIMStatus status);
}
