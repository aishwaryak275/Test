package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.GenerateCycleRequest;
import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.DuplicateResourceException;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.repository.BillingCycleRepository;
import com.teleConnect.billing.inovice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingCycleService Tests")
class BillingCycleServiceTest {

    @Mock private BillingCycleRepository cycleRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @InjectMocks private BillingCycleServiceImpl billingCycleService;

    private BillingCycle openCycle;
    private BillingCycle generatedCycle;
    private BillingCycle closedCycle;

    @BeforeEach
    void setUp() {
        openCycle = BillingCycle.builder()
                .cycleID(1L).accountID(1001L)
                .cycleStart(LocalDate.of(2026, 5, 1))
                .cycleEnd(LocalDate.of(2026, 5, 31))
                .status(CycleStatus.Open).build();

        generatedCycle = BillingCycle.builder()
                .cycleID(2L).accountID(1002L)
                .cycleStart(LocalDate.of(2026, 4, 1))
                .cycleEnd(LocalDate.of(2026, 4, 30))
                .status(CycleStatus.Generated).build();

        closedCycle = BillingCycle.builder()
                .cycleID(3L).accountID(1003L)
                .cycleStart(LocalDate.of(2026, 3, 1))
                .cycleEnd(LocalDate.of(2026, 3, 31))
                .status(CycleStatus.Closed).build();
    }

    // ── generateInvoices ─────────────────────────────────────────────────

    @Test @DisplayName("generateInvoices — returns non-null on success")
    void generateInvoices_success() {
        GenerateCycleRequest req = new GenerateCycleRequest("2026-05-31", new java.math.BigDecimal("500"), new java.math.BigDecimal("200"), new java.math.BigDecimal("100"), new java.math.BigDecimal("149.32"));
        when(cycleRepository.findByStatusAndCycleEndLessThanEqual(CycleStatus.Open, LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(openCycle));
        when(invoiceRepository.findByCycle_CycleID(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = billingCycleService.generateInvoices(req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("generateInvoices — saves invoice for each open cycle")
    void generateInvoices_savesInvoice() {
        GenerateCycleRequest req = new GenerateCycleRequest("2026-05-31", new java.math.BigDecimal("500"), new java.math.BigDecimal("200"), new java.math.BigDecimal("100"), new java.math.BigDecimal("149.32"));
        when(cycleRepository.findByStatusAndCycleEndLessThanEqual(CycleStatus.Open, LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(openCycle));
        when(invoiceRepository.findByCycle_CycleID(1L)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        billingCycleService.generateInvoices(req);
        verify(invoiceRepository, times(1)).save(any());
    }

    @Test @DisplayName("generateInvoices — throws DuplicateResourceException when invoice already exists")
    void generateInvoices_skipsExistingInvoice() {
        GenerateCycleRequest req = new GenerateCycleRequest("2026-04-30", new java.math.BigDecimal("500"), new java.math.BigDecimal("200"), new java.math.BigDecimal("100"), new java.math.BigDecimal("149.32"));
        Invoice existing = Invoice.builder().invoiceID(10L).status(InvoiceStatus.Generated).build();
        when(cycleRepository.findByStatusAndCycleEndLessThanEqual(CycleStatus.Open, LocalDate.of(2026, 4, 30)))
                .thenReturn(List.of(openCycle));
        when(invoiceRepository.findByCycle_CycleID(1L)).thenReturn(Optional.of(existing));
        assertThrows(DuplicateResourceException.class, () -> billingCycleService.generateInvoices(req));
    }

    @Test @DisplayName("generateInvoices — throws BusinessRuleException when no eligible cycles")
    void generateInvoices_noOpenCycles() {
        GenerateCycleRequest req = new GenerateCycleRequest("2026-05-31", new java.math.BigDecimal("500"), new java.math.BigDecimal("200"), new java.math.BigDecimal("100"), new java.math.BigDecimal("149.32"));
        when(cycleRepository.findByStatusAndCycleEndLessThanEqual(any(), any())).thenReturn(List.of());
        assertThrows(BusinessRuleException.class, () -> billingCycleService.generateInvoices(req));
    }

    // ── closeCycle ────────────────────────────────────────────────────────

    @Test @DisplayName("closeCycle — success on Generated cycle")
    void closeCycle_success() {
        when(cycleRepository.findById(2L)).thenReturn(Optional.of(generatedCycle));
        when(cycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = billingCycleService.closeCycle(2L);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("closeCycle — throws ResourceNotFoundException for unknown cycle")
    void closeCycle_notFound() {
        when(cycleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> billingCycleService.closeCycle(99L));
    }

    @Test @DisplayName("closeCycle — throws BusinessRuleException on Closed cycle")
    void closeCycle_alreadyClosed() {
        when(cycleRepository.findById(3L)).thenReturn(Optional.of(closedCycle));
        assertThrows(BusinessRuleException.class, () -> billingCycleService.closeCycle(3L));
    }

    @Test @DisplayName("closeCycle — throws BusinessRuleException on Open cycle")
    void closeCycle_openCycle() {
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        assertThrows(BusinessRuleException.class, () -> billingCycleService.closeCycle(1L));
    }

    @Test @DisplayName("closeCycle — saves cycle after closing")
    void closeCycle_savesCycle() {
        when(cycleRepository.findById(2L)).thenReturn(Optional.of(generatedCycle));
        when(cycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        billingCycleService.closeCycle(2L);
        verify(cycleRepository, times(1)).save(any());
    }

    // ── getCycleById ──────────────────────────────────────────────────────

    @Test @DisplayName("getCycleById — returns 200 response")
    void getCycleById_success() {
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        var response = billingCycleService.getCycleById(1L);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("getCycleById — throws ResourceNotFoundException for unknown id")
    void getCycleById_notFound() {
        when(cycleRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> billingCycleService.getCycleById(99L));
    }

    @Test @DisplayName("getCycleById — response data cycleID matches")
    void getCycleById_dataMatches() {
        when(cycleRepository.findById(1L)).thenReturn(Optional.of(openCycle));
        var response = billingCycleService.getCycleById(1L);
        assertEquals(1L, response.getData().getCycleID());
    }

    // ── getCyclesByAccount ────────────────────────────────────────────────

    @Test @DisplayName("getCyclesByAccount — returns 200 with page")
    void getCyclesByAccount_success() {
        var pageable = PageRequest.of(0, 10);
        when(cycleRepository.findByAccountID(1001L, pageable))
                .thenReturn(new PageImpl<>(List.of(openCycle), pageable, 1));
        var response = billingCycleService.getCyclesByAccount(1001L, null, pageable);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("getCyclesByAccount — empty page returns 200")
    void getCyclesByAccount_emptyPage() {
        var pageable = PageRequest.of(0, 10);
        when(cycleRepository.findByAccountID(1001L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        var response = billingCycleService.getCyclesByAccount(1001L, null, pageable);
        assertEquals(200, response.getStatusCode());
    }
}
