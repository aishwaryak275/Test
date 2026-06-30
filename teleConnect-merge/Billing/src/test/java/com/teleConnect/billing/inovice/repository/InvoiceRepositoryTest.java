package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("InvoiceRepository Tests")
class InvoiceRepositoryTest {

    @Autowired InvoiceRepository invoiceRepository;
    @Autowired BillingCycleRepository cycleRepository;

    private BillingCycle savedCycle;

    @BeforeEach
    void clean() {
        invoiceRepository.deleteAll();
        cycleRepository.deleteAll();

        BillingCycle c = new BillingCycle();
        c.setAccountID(1001L); c.setStatus(CycleStatus.Generated);
        c.setCycleStart(LocalDate.of(2026, 5, 1)); c.setCycleEnd(LocalDate.of(2026, 5, 31));
        savedCycle = cycleRepository.save(c);
    }

    private Invoice makeInvoice(Long accountId, InvoiceStatus status, LocalDate dueDate) {
        Invoice inv = new Invoice();
        inv.setAccountID(accountId); inv.setStatus(status);
        inv.setDueDate(dueDate); inv.setTotalAmount(BigDecimal.valueOf(500));
        inv.setCycle(savedCycle);
        return inv;
    }

    @Test @DisplayName("save — id auto-generated")
    void save_idGenerated() {
        Invoice inv = invoiceRepository.save(makeInvoice(1001L, InvoiceStatus.Generated, LocalDate.of(2026, 6, 15)));
        assertThat(inv.getInvoiceID()).isNotNull();
    }

    @Test @DisplayName("findById — returns saved invoice")
    void findById_found() {
        Invoice saved = invoiceRepository.save(makeInvoice(1001L, InvoiceStatus.Generated, LocalDate.of(2026, 6, 15)));
        assertThat(invoiceRepository.findById(saved.getInvoiceID())).isPresent();
    }

    @Test @DisplayName("findById — empty for unknown id")
    void findById_notFound() {
        assertThat(invoiceRepository.findById(99999L)).isEmpty();
    }

    @Test @DisplayName("delete — invoice no longer found")
    void delete_removed() {
        Invoice saved = invoiceRepository.save(makeInvoice(1001L, InvoiceStatus.Generated, LocalDate.of(2026, 6, 15)));
        invoiceRepository.deleteById(saved.getInvoiceID());
        assertThat(invoiceRepository.findById(saved.getInvoiceID())).isEmpty();
    }

    @Test @DisplayName("findByAccountIDAndDueDateBetween — returns invoice in range")
    void findByAccountAndDate_match() {
        invoiceRepository.save(makeInvoice(1001L, InvoiceStatus.Generated, LocalDate.of(2026, 6, 15)));
        Page<?> result = invoiceRepository.findByAccountIDAndDueDateBetween(
                1001L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("findByAccountIDAndDueDateBetween — excludes out-of-range invoice")
    void findByAccountAndDate_noMatch() {
        invoiceRepository.save(makeInvoice(1001L, InvoiceStatus.Generated, LocalDate.of(2026, 7, 15)));
        Page<?> result = invoiceRepository.findByAccountIDAndDueDateBetween(
                1001L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), PageRequest.of(0, 10));
        assertThat(result).isEmpty();
    }

    @Test @DisplayName("findByAccountIDAndDueDateBetween — excludes different account")
    void findByAccountAndDate_differentAccount() {
        invoiceRepository.save(makeInvoice(1001L, InvoiceStatus.Generated, LocalDate.of(2026, 6, 15)));
        Page<?> result = invoiceRepository.findByAccountIDAndDueDateBetween(
                9999L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), PageRequest.of(0, 10));
        assertThat(result).isEmpty();
    }

    @Test @DisplayName("findByCycle_CycleID — returns invoice for cycle")
    void findByCycleID_found() {
        invoiceRepository.save(makeInvoice(1001L, InvoiceStatus.Generated, LocalDate.of(2026, 6, 15)));
        Optional<Invoice> result = invoiceRepository.findByCycle_CycleID(savedCycle.getCycleID());
        assertThat(result).isPresent();
    }

    @Test @DisplayName("findByCycle_CycleID — empty for unknown cycle")
    void findByCycleID_notFound() {
        Optional<Invoice> result = invoiceRepository.findByCycle_CycleID(99999L);
        assertThat(result).isEmpty();
    }

    @Test @DisplayName("findByCycle_CycleID — empty when no invoice linked")
    void findByCycleID_noInvoice() {
        BillingCycle newCycle = new BillingCycle();
        newCycle.setAccountID(2002L); newCycle.setStatus(CycleStatus.Open);
        newCycle.setCycleStart(LocalDate.of(2026, 6, 1)); newCycle.setCycleEnd(LocalDate.of(2026, 6, 30));
        BillingCycle saved = cycleRepository.save(newCycle);
        assertThat(invoiceRepository.findByCycle_CycleID(saved.getCycleID())).isEmpty();
    }
}
