package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.CreateDisputeRequest;
import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.BillingDispute;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import com.teleConnect.billing.inovice.entity.enums.DisputeStatus;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.repository.BillingDisputeRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Billing Dispute Service Tests")
class BillingDisputeServiceTest {

    @Mock private BillingDisputeRepository disputeRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @InjectMocks private BillingDisputeServiceImpl disputeService;

    private BillingCycle cycle;
    private Invoice generatedInvoice;
    private Invoice paidInvoice;

    @BeforeEach
    void setUp() {
        cycle = BillingCycle.builder()
                .cycleID(1L).accountID(1001L)
                .cycleStart(LocalDate.of(2026, 5, 1))
                .cycleEnd(LocalDate.of(2026, 5, 31))
                .status(CycleStatus.Generated).build();

        generatedInvoice = Invoice.builder()
                .invoiceID(1L).accountID(1001L).cycle(cycle)
                .totalAmount(new BigDecimal("949.32"))
                .dueDate(LocalDate.of(2026, 6, 22))
                .status(InvoiceStatus.Generated).build();

        paidInvoice = Invoice.builder()
                .invoiceID(4L).accountID(1001L).cycle(cycle)
                .totalAmount(new BigDecimal("949.32"))
                .dueDate(LocalDate.of(2026, 6, 22))
                .status(InvoiceStatus.Paid).build();
    }

    private BillingDispute makeDispute(Long id) {
        BillingDispute d = new BillingDispute();
        d.setDisputeID(id);
        d.setInvoice(generatedInvoice);
        d.setSubscriberID(1001L);
        d.setDisputedAmount(new BigDecimal("100"));
        d.setDisputeReason("Billing error");
        d.setStatus(DisputeStatus.Open);
        d.setRaisedDate(LocalDateTime.of(2026, 6, 10, 10, 0));
        return d;
    }

    // ── createDispute ─────────────────────────────────────────────────────

    @Test @DisplayName("createDispute — returns 201 on success")
    void createDispute_success() {
        CreateDisputeRequest req = new CreateDisputeRequest(1L, "Billing error", new BigDecimal("100"), "desc");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = disputeService.createDispute(req);
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
    }

    @Test @DisplayName("createDispute — throws ResourceNotFoundException for unknown invoice")
    void createDispute_invoiceNotFound() {
        CreateDisputeRequest req = new CreateDisputeRequest(99L, "reason", new BigDecimal("100"), "desc");
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> disputeService.createDispute(req));
    }

    @Test @DisplayName("createDispute — throws BusinessRuleException when amount exceeds invoice total")
    void createDispute_amountExceedsTotal() {
        CreateDisputeRequest req = new CreateDisputeRequest(1L, "reason", new BigDecimal("9999"), "desc");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        assertThrows(BusinessRuleException.class, () -> disputeService.createDispute(req));
    }

    @Test @DisplayName("createDispute — saves dispute to repository")
    void createDispute_savesDispute() {
        CreateDisputeRequest req = new CreateDisputeRequest(1L, "reason", new BigDecimal("100"), "desc");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        when(disputeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        disputeService.createDispute(req);
        verify(disputeRepository, times(1)).save(any());
    }

    @Test @DisplayName("createDispute — throws BusinessRuleException on Paid invoice")
    void createDispute_paidInvoice() {
        CreateDisputeRequest req = new CreateDisputeRequest(4L, "reason", new BigDecimal("100"), "desc");
        when(invoiceRepository.findById(4L)).thenReturn(Optional.of(paidInvoice));
        assertThrows(BusinessRuleException.class, () -> disputeService.createDispute(req));
    }

    // ── getDisputeById ────────────────────────────────────────────────────

    @Test @DisplayName("getDisputeById — returns 200 response")
    void getDisputeById_success() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(makeDispute(1L)));
        var response = disputeService.getDisputeById(1L);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("getDisputeById — throws ResourceNotFoundException for unknown id")
    void getDisputeById_notFound() {
        when(disputeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> disputeService.getDisputeById(99L));
    }

    @Test @DisplayName("getDisputeById — response data has correct disputeID")
    void getDisputeById_dataMatches() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(makeDispute(1L)));
        var response = disputeService.getDisputeById(1L);
        assertEquals(1L, response.getData().getDisputeID());
    }

    @Test @DisplayName("getDisputeById — response data has disputeReason")
    void getDisputeById_reasonMatches() {
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(makeDispute(1L)));
        var response = disputeService.getDisputeById(1L);
        assertEquals("Billing error", response.getData().getDisputeReason());
    }

    // ── getDisputesByAccount ──────────────────────────────────────────────

    @Test @DisplayName("getDisputesByAccount — returns 200 with page data")
    void getDisputesByAccount_success() {
        var pageable = PageRequest.of(0, 10);
        when(disputeRepository.findByInvoice_AccountID(1001L, pageable))
                .thenReturn(new PageImpl<>(List.of(makeDispute(1L)), pageable, 1));
        var response = disputeService.getDisputesByAccount(1001L, null, pageable);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("getDisputesByAccount — empty page returns 200")
    void getDisputesByAccount_emptyPage() {
        var pageable = PageRequest.of(0, 10);
        when(disputeRepository.findByInvoice_AccountID(1001L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        var response = disputeService.getDisputesByAccount(1001L, null, pageable);
        assertEquals(200, response.getStatusCode());
    }
}
