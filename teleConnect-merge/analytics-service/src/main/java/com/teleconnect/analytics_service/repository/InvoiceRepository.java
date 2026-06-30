package com.teleconnect.analytics_service.repository;

import com.teleconnect.analytics_service.entity.Invoice;
import com.teleconnect.analytics_service.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByCycleIdAndStatusIn(Long cycleId, List<InvoiceStatus> statuses);

    List<Invoice> findByAccountId(Long accountId);

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.cycleId = :cycleId " +
            "AND i.status IN ('PAID','OVERDUE','SENT')")
    BigDecimal sumTotalAmountByCycle(@Param("cycleId") Long cycleId);

    @Query("SELECT SUM(i.paidAmount) FROM Invoice i WHERE i.cycleId = :cycleId " +
            "AND i.status = 'PAID'")
    BigDecimal sumPaidAmountByCycle(@Param("cycleId") Long cycleId);

    @Query("SELECT COUNT(DISTINCT i.accountId) FROM Invoice i WHERE i.cycleId = :cycleId " +
            "AND i.status IN ('PAID','OVERDUE','SENT')")
    long countDistinctAccountsByCycle(@Param("cycleId") Long cycleId);

    @Query("SELECT i FROM Invoice i WHERE i.status = 'OVERDUE' AND i.dueDate < :today")
    List<Invoice> findAllOverdue(@Param("today") LocalDate today);

    @Query("SELECT i FROM Invoice i WHERE i.status = 'OVERDUE' AND i.dueDate BETWEEN :start AND :end")
    List<Invoice> findOverdueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
