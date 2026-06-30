package com.teleConnect.billing.inovice.repository;

import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("BillingCycleRepository Tests")
class BillingCycleRepositoryTest {

    @Autowired BillingCycleRepository cycleRepository;

    @BeforeEach
    void clean() { cycleRepository.deleteAll(); }

    private BillingCycle makeCycle(Long accountId, CycleStatus status, LocalDate start, LocalDate end) {
        BillingCycle c = new BillingCycle();
        c.setAccountID(accountId); c.setStatus(status);
        c.setCycleStart(start); c.setCycleEnd(end);
        return c;
    }

    // ── save / findById ──────────────────────────────────────────────────

    @Test @DisplayName("save — id auto-generated")
    void save_idGenerated() {
        BillingCycle c = cycleRepository.save(
                makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        assertThat(c.getCycleID()).isNotNull();
    }

    @Test @DisplayName("findById — returns saved cycle")
    void findById_found() {
        BillingCycle saved = cycleRepository.save(
                makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        assertThat(cycleRepository.findById(saved.getCycleID())).isPresent();
    }

    @Test @DisplayName("findById — empty for unknown id")
    void findById_notFound() {
        assertThat(cycleRepository.findById(99999L)).isEmpty();
    }

    @Test @DisplayName("delete — cycle no longer found")
    void delete_removed() {
        BillingCycle saved = cycleRepository.save(
                makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        cycleRepository.deleteById(saved.getCycleID());
        assertThat(cycleRepository.findById(saved.getCycleID())).isEmpty();
    }

    // ── findByStatusAndCycleEndLessThanEqual ─────────────────────────────

    @Test @DisplayName("findByStatusAndCycleEnd — returns Open cycle before date")
    void findByStatusAndEnd_match() {
        cycleRepository.save(makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        List<BillingCycle> result = cycleRepository.findByStatusAndCycleEndLessThanEqual(
                CycleStatus.Open, LocalDate.of(2026, 5, 31));
        assertThat(result).hasSize(1);
    }

    @Test @DisplayName("findByStatusAndCycleEnd — excludes cycle after date")
    void findByStatusAndEnd_excludesFuture() {
        cycleRepository.save(makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)));
        List<BillingCycle> result = cycleRepository.findByStatusAndCycleEndLessThanEqual(
                CycleStatus.Open, LocalDate.of(2026, 5, 31));
        assertThat(result).isEmpty();
    }

    @Test @DisplayName("findByStatusAndCycleEnd — excludes non-Open status")
    void findByStatusAndEnd_excludesClosedStatus() {
        cycleRepository.save(makeCycle(1001L, CycleStatus.Closed, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        List<BillingCycle> result = cycleRepository.findByStatusAndCycleEndLessThanEqual(
                CycleStatus.Open, LocalDate.of(2026, 5, 31));
        assertThat(result).isEmpty();
    }

    // ── findByAccountID (pageable) ────────────────────────────────────────

    @Test @DisplayName("findByAccountID — returns cycles for account")
    void findByAccountID_match() {
        cycleRepository.save(makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        Page<BillingCycle> result = cycleRepository.findByAccountID(1001L, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("findByAccountID — empty for different account")
    void findByAccountID_noMatch() {
        cycleRepository.save(makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        Page<BillingCycle> result = cycleRepository.findByAccountID(9999L, PageRequest.of(0, 10));
        assertThat(result).isEmpty();
    }

    @Test @DisplayName("findByAccountID — page size respected")
    void findByAccountID_pageSizeRespected() {
        for (int i = 0; i < 5; i++) {
            cycleRepository.save(makeCycle(1001L, CycleStatus.Open,
                    LocalDate.of(2026, i + 1, 1), LocalDate.of(2026, i + 1, 28)));
        }
        Page<BillingCycle> result = cycleRepository.findByAccountID(1001L, PageRequest.of(0, 2));
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    // ── findByAccountIDAndStatus ──────────────────────────────────────────

    @Test @DisplayName("findByAccountIDAndStatus — returns only matching status")
    void findByAccountIDAndStatus_match() {
        cycleRepository.save(makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        cycleRepository.save(makeCycle(1001L, CycleStatus.Closed, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)));
        Page<BillingCycle> result = cycleRepository.findByAccountIDAndStatus(1001L, CycleStatus.Open, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test @DisplayName("findByAccountIDAndStatus — empty when status not present")
    void findByAccountIDAndStatus_noMatch() {
        cycleRepository.save(makeCycle(1001L, CycleStatus.Open, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
        Page<BillingCycle> result = cycleRepository.findByAccountIDAndStatus(1001L, CycleStatus.Closed, PageRequest.of(0, 10));
        assertThat(result).isEmpty();
    }
}
