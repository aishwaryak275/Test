package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.BillingDispute;
import com.teleConnect.billing.inovice.entity.enums.DisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillingDisputeRepository extends JpaRepository<BillingDispute, Long> {

    // Used by GET /disputes/account/{accountId}
    Page<BillingDispute> findByInvoice_AccountID(Long accountID, Pageable pageable);

    Page<BillingDispute> findByInvoice_AccountIDAndStatus(Long accountID, DisputeStatus status, Pageable pageable);
}
