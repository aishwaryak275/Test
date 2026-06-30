package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Used by GET /invoices/account/{accountId}
    Page<Invoice> findByAccountID(Long accountID, Pageable pageable);

    Page<Invoice> findByAccountIDAndStatus(Long accountID, InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByAccountIDAndDueDateBetween(Long accountID, LocalDate from, LocalDate to, Pageable pageable);

    // Check for duplicate invoice per cycle
    Optional<Invoice> findByCycle_CycleID(Long cycleID);
}
