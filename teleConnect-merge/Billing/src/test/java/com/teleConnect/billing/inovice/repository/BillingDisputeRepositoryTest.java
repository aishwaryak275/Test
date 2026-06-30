package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.BillingDispute;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import com.teleConnect.billing.inovice.entity.enums.DisputeStatus;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("BillingDisputeRepository Tests")
class BillingDisputeRepositoryTest {

    @Autowired BillingDisputeRepository disputeRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired BillingCycleRepository cycleRepository;

    private Invoice savedInvoice;

    @BeforeEach
    void clean() {
        disputeRepository.deleteAll();
        invoiceRepository.deleteAll();
        cycleRepository.deleteAll();

        BillingCycle c = new BillingCycle();
        c.setAccountID(1001L); c.setStatus(CycleStatus.Generated);
        c.setCycleStart(LocalDate.of(2026, 5, 1)); c.setCycleEnd(LocalDate.of(2026, 5, 31));
        BillingCycle savedCycle = cycleRepository.save(c);

        Invoice inv = new Invoice();
        inv.setAccountID(1001L); inv.setStatus(InvoiceStatus.Generated);
        inv.setDueDate(LocalDate.of(2026, 6, 15)); inv.setTotalAmount(BigDecimal.valueOf(500));
        inv.setCycle(savedCycle);
        savedInvoice = invoiceRepository.save(inv);
    }

    private BillingDispute makeDispute(BigDecimal amount, String reason, DisputeStatus status) {
        BillingDispute d = new BillingDispute();
        d.setInvoice(savedInvoice); d.setSubscriberID(1001L);
        d.setDisputedAmount(amount); d.setDisputeReason(reason);
        d.setDescription("desc"); d.setStatus(status);
        d.setRaisedDate(LocalDateTime.of(2026, 6, 10, 10, 0));
        return d;
    }

    @Test @DisplayName("save — id auto-generated")
    void save_idGenerated() {
        BillingDispute d = disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "reason", DisputeStatus.Open));
        assertThat(d.getDisputeID()).isNotNull();
    }

    @Test @DisplayName("findById — returns saved dispute")
    void findById_found() {
        BillingDispute saved = disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "reason", DisputeStatus.Open));
        assertThat(disputeRepository.findById(saved.getDisputeID())).isPresent();
    }

    @Test @DisplayName("findById — empty for unknown id")
    void findById_notFound() {
        assertThat(disputeRepository.findById(99999L)).isEmpty();
    }

    @Test @DisplayName("delete — dispute no longer found")
    void delete_removed() {
        BillingDispute saved = disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "reason", DisputeStatus.Open));
        disputeRepository.deleteById(saved.getDisputeID());
        assertThat(disputeRepository.findById(saved.getDisputeID())).isEmpty();
    }

    @Test @DisplayName("findByInvoice_AccountID — returns dispute for account")
    void findByAccountID_match() {
        disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "reason", DisputeStatus.Open));
        Page<?> result = disputeRepository.findByInvoice_AccountID(1001L, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("findByInvoice_AccountID — empty for different account")
    void findByAccountID_noMatch() {
        disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "reason", DisputeStatus.Open));
        Page<?> result = disputeRepository.findByInvoice_AccountID(9999L, PageRequest.of(0, 10));
        assertThat(result).isEmpty();
    }

    @Test @DisplayName("findByInvoice_AccountID — returns multiple disputes")
    void findByAccountID_multiple() {
        disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "r1", DisputeStatus.Open));
        disputeRepository.save(makeDispute(BigDecimal.valueOf(200), "r2", DisputeStatus.Resolved));
        Page<?> result = disputeRepository.findByInvoice_AccountID(1001L, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test @DisplayName("findByInvoice_AccountIDAndStatus — returns only Open")
    void findByAccountIDAndStatus_open() {
        disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "r1", DisputeStatus.Open));
        disputeRepository.save(makeDispute(BigDecimal.valueOf(200), "r2", DisputeStatus.Resolved));
        Page<?> result = disputeRepository.findByInvoice_AccountIDAndStatus(1001L, DisputeStatus.Open, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("findByInvoice_AccountIDAndStatus — empty when status not present")
    void findByAccountIDAndStatus_noMatch() {
        disputeRepository.save(makeDispute(BigDecimal.valueOf(100), "r1", DisputeStatus.Open));
        Page<?> result = disputeRepository.findByInvoice_AccountIDAndStatus(1001L, DisputeStatus.Rejected, PageRequest.of(0, 10));
        assertThat(result).isEmpty();
    }
}
