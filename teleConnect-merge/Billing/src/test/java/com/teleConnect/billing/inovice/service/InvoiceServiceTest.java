package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.LateFeeRequest;
import com.teleConnect.billing.inovice.dto.request.PayInvoiceRequest;
import com.teleConnect.billing.inovice.dto.request.WaiveLateFeeRequest;
import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.entity.enums.PaymentMethod;
import com.teleConnect.billing.inovice.entity.enums.PaymentStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.DuplicateResourceException;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.repository.InvoiceRepository;
import com.teleConnect.billing.inovice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvoiceService Tests")
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @InjectMocks private InvoiceServiceImpl invoiceService;

    private BillingCycle cycle;
    private Invoice generatedInvoice;
    private Invoice overdueInvoice;
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
                .dueDate(LocalDate.of(2026, 6, 15))
                .status(InvoiceStatus.Generated).build();

        overdueInvoice = Invoice.builder()
                .invoiceID(2L).accountID(1001L).cycle(cycle)
                .totalAmount(new BigDecimal("949.32"))
                .dueDate(LocalDate.of(2026, 5, 15))
                .status(InvoiceStatus.Overdue).build();

        paidInvoice = Invoice.builder()
                .invoiceID(3L).accountID(1001L).cycle(cycle)
                .totalAmount(new BigDecimal("949.32"))
                .dueDate(LocalDate.of(2026, 6, 15))
                .status(InvoiceStatus.Paid).build();
    }

    // ── getInvoiceById ──────────────────────────────────────────────────

    @Test @DisplayName("getInvoiceById — returns 200 response")
    void getInvoiceById_success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        var response = invoiceService.getInvoiceById(1L);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("getInvoiceById — throws ResourceNotFoundException for unknown id")
    void getInvoiceById_notFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById(99L));
    }

    @Test @DisplayName("getInvoiceById — response data invoiceID matches")
    void getInvoiceById_dataMatches() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        var response = invoiceService.getInvoiceById(1L);
        assertEquals(1L, response.getData().getInvoiceID());
    }

    // ── recordPayment ────────────────────────────────────────────────────

    @Test @DisplayName("recordPayment — success on Generated invoice")
    void recordPayment_success() {
        PayInvoiceRequest req = new PayInvoiceRequest(new BigDecimal("949.32"), "UPI", "TXN-001");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        when(paymentRepository.findByTransactionRef("TXN-001")).thenReturn(Optional.empty());
        when(paymentRepository.sumPaidAmountByInvoiceId(1L, PaymentStatus.Success)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = invoiceService.recordPayment(1L, req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("recordPayment — throws ResourceNotFoundException if invoice missing")
    void recordPayment_invoiceNotFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.recordPayment(99L, new PayInvoiceRequest(BigDecimal.TEN, "UPI", "T1")));
    }

    @Test @DisplayName("recordPayment — throws DuplicateResourceException on duplicate transactionRef")
    void recordPayment_duplicateTxRef() {
        PayInvoiceRequest req = new PayInvoiceRequest(new BigDecimal("100"), "UPI", "TXN-DUP");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        when(paymentRepository.findByTransactionRef("TXN-DUP")).thenReturn(Optional.of(mock(com.teleConnect.billing.inovice.entity.Payment.class)));
        assertThrows(DuplicateResourceException.class, () -> invoiceService.recordPayment(1L, req));
    }

    @Test @DisplayName("recordPayment — throws BusinessRuleException on invalid paymentMethod")
    void recordPayment_invalidMethod() {
        PayInvoiceRequest req = new PayInvoiceRequest(new BigDecimal("100"), "BITCOIN", "TXN-002");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        when(paymentRepository.findByTransactionRef("TXN-002")).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class, () -> invoiceService.recordPayment(1L, req));
    }

    @Test @DisplayName("recordPayment — saves payment to repository")
    void recordPayment_savesPayment() {
        PayInvoiceRequest req = new PayInvoiceRequest(new BigDecimal("949.32"), "Card", "TXN-003");
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(generatedInvoice));
        when(paymentRepository.findByTransactionRef("TXN-003")).thenReturn(Optional.empty());
        when(paymentRepository.sumPaidAmountByInvoiceId(1L, PaymentStatus.Success)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        invoiceService.recordPayment(1L, req);
        verify(paymentRepository, times(1)).save(any());
    }

    // ── applyLateFee ─────────────────────────────────────────────────────

    @Test @DisplayName("applyLateFee — success on Overdue invoice")
    void applyLateFee_success() {
        LateFeeRequest req = new LateFeeRequest(new BigDecimal("50"), "overdue");
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(overdueInvoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = invoiceService.applyLateFee(2L, req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("applyLateFee — throws ResourceNotFoundException if invoice missing")
    void applyLateFee_notFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.applyLateFee(99L, new LateFeeRequest(BigDecimal.TEN, "late")));
    }

    @Test @DisplayName("applyLateFee — throws BusinessRuleException on Paid invoice")
    void applyLateFee_paidInvoice() {
        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(paidInvoice));
        assertThrows(BusinessRuleException.class,
                () -> invoiceService.applyLateFee(3L, new LateFeeRequest(BigDecimal.TEN, "late")));
    }

    // ── waiveLateFee ─────────────────────────────────────────────────────

    @Test @DisplayName("waiveLateFee — success on Overdue invoice")
    void waiveLateFee_success() {
        WaiveLateFeeRequest req = new WaiveLateFeeRequest("goodwill", "manager");
        when(invoiceRepository.findById(2L)).thenReturn(Optional.of(overdueInvoice));
        when(invoiceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var response = invoiceService.waiveLateFee(2L, req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("waiveLateFee — throws ResourceNotFoundException if invoice missing")
    void waiveLateFee_notFound() {
        when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.waiveLateFee(99L, new WaiveLateFeeRequest("r", "a")));
    }

    // ── getInvoicesByAccount ─────────────────────────────────────────────

    @Test @DisplayName("getInvoicesByAccount — returns 200 with page data")
    void getInvoicesByAccount_success() {
        var pageable = PageRequest.of(0, 5);
        when(invoiceRepository.findByAccountID(1001L, pageable))
                .thenReturn(new PageImpl<>(List.of(generatedInvoice), pageable, 1));
        var response = invoiceService.getInvoicesByAccount(1001L, null, pageable);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
    }

    @Test @DisplayName("getInvoicesByAccount — empty page returns 200")
    void getInvoicesByAccount_emptyPage() {
        var pageable = PageRequest.of(0, 5);
        when(invoiceRepository.findByAccountID(1001L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));
        var response = invoiceService.getInvoicesByAccount(1001L, null, pageable);
        assertEquals(200, response.getStatusCode());
    }
}
