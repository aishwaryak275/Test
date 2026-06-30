package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.Payment;
import com.teleConnect.billing.inovice.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Check for duplicate transactionRef — spec says 409 Conflict if not unique
    Optional<Payment> findByTransactionRef(String transactionRef);

    // Sum of all successful payments for an invoice (uses named param for enum — Hibernate 7 requirement)
    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.invoice.invoiceID = :invoiceId AND p.status = :status")
    BigDecimal sumPaidAmountByInvoiceId(@Param("invoiceId") Long invoiceId, @Param("status") PaymentStatus status);
}
