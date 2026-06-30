package com.teleConnect.billing.inovice.controller;

import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.InvoiceResponse;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.GlobalExceptionHandler;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoiceController Tests")
class InvoiceControllerTest {

    @Mock InvoiceService invoiceService;
    @InjectMocks InvoiceController controller;

    MockMvc mockMvc;
    private static final String BASE = "/teleConnect/billing/invoices";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private InvoiceResponse sample() {
        return InvoiceResponse.builder().invoiceID(1L).accountID(1001L)
                .totalAmount(BigDecimal.valueOf(500)).dueDate(LocalDate.of(2026, 6, 30))
                .status(InvoiceStatus.Generated).build();
    }

    private String payBody() {
        return "{\"amountPaid\":500.00,\"paymentMethod\":\"UPI\",\"transactionRef\":\"TXN-001\"}";
    }

    private String lateFeeBody() { return "{\"feeAmount\":50.00,\"reason\":\"overdue\"}"; }

    private String waiveBody() { return "{\"waiverReason\":\"goodwill\",\"authorisedBy\":\"manager\"}"; }

    // ── GET /{invoiceId} ─────────────────────────────────────────────────

    @Test @DisplayName("GET /{invoiceId} — 200 OK")
    void getById_200() throws Exception {
        when(invoiceService.getInvoiceById(1L)).thenReturn(new ApiResponse<>(200, "ok", sample()));
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isOk());
    }

    @Test @DisplayName("GET /{invoiceId} — 404 when not found")
    void getById_404() throws Exception {
        when(invoiceService.getInvoiceById(99L)).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get(BASE + "/99")).andExpect(status().isNotFound());
    }

    @Test @DisplayName("GET /{invoiceId} — response has data field")
    void getById_hasData() throws Exception {
        when(invoiceService.getInvoiceById(1L)).thenReturn(new ApiResponse<>(200, "ok", sample()));
        mockMvc.perform(get(BASE + "/1")).andExpect(jsonPath("$.data").exists());
    }

    @Test @DisplayName("GET /{invoiceId} — 500 on unexpected error")
    void getById_500() throws Exception {
        when(invoiceService.getInvoiceById(1L)).thenThrow(new RuntimeException("boom"));
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isInternalServerError());
    }

    // ── GET /account/{accountId} ─────────────────────────────────────────

    @Test @DisplayName("GET /account/{accountId} — 200 OK")
    void getByAccount_200() throws Exception {
        when(invoiceService.getInvoicesByAccount(eq(1001L), isNull(), any(Pageable.class)))
                .thenReturn(new ApiResponse<>(200, "ok", new PageImpl<>(List.of(), PageRequest.of(0, 5), 0)));
        mockMvc.perform(get(BASE + "/account/1001")).andExpect(status().isOk());
    }

    @Test @DisplayName("GET /account/{accountId} — 500 on error")
    void getByAccount_500() throws Exception {
        when(invoiceService.getInvoicesByAccount(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("boom"));
        mockMvc.perform(get(BASE + "/account/1001")).andExpect(status().isInternalServerError());
    }

    // ── POST /{invoiceId}/pay ────────────────────────────────────────────

    @Test @DisplayName("POST /pay — 200 OK")
    void pay_200() throws Exception {
        when(invoiceService.recordPayment(eq(1L), any()))
                .thenReturn(new ApiResponse<>(200, "Payment recorded", null));
        mockMvc.perform(post(BASE + "/1/pay").contentType(MediaType.APPLICATION_JSON).content(payBody()))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("POST /pay — 400 on validation failure (missing fields)")
    void pay_400_validation() throws Exception {
        mockMvc.perform(post(BASE + "/1/pay").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST /pay — 404 when invoice not found")
    void pay_404() throws Exception {
        when(invoiceService.recordPayment(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Invoice not found"));
        mockMvc.perform(post(BASE + "/99/pay").contentType(MediaType.APPLICATION_JSON).content(payBody()))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("POST /pay — 400 on BusinessRuleException")
    void pay_businessRule() throws Exception {
        when(invoiceService.recordPayment(eq(1L), any()))
                .thenThrow(new BusinessRuleException("Invoice already paid"));
        mockMvc.perform(post(BASE + "/1/pay").contentType(MediaType.APPLICATION_JSON).content(payBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invoice already paid"));
    }

    @Test @DisplayName("POST /pay — service not called on validation failure")
    void pay_serviceNotCalled() throws Exception {
        mockMvc.perform(post(BASE + "/1/pay").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(invoiceService, never()).recordPayment(anyLong(), any());
    }

    // ── POST /{invoiceId}/latefee ────────────────────────────────────────

    @Test @DisplayName("POST /latefee — 200 OK")
    void latefee_200() throws Exception {
        when(invoiceService.applyLateFee(eq(1L), any()))
                .thenReturn(new ApiResponse<>(200, "Late fee applied", null));
        mockMvc.perform(post(BASE + "/1/latefee").contentType(MediaType.APPLICATION_JSON).content(lateFeeBody()))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("POST /latefee — 400 on missing feeAmount")
    void latefee_400() throws Exception {
        mockMvc.perform(post(BASE + "/1/latefee").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"late\"}")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST /latefee — 400 on BusinessRuleException")
    void latefee_businessRule() throws Exception {
        when(invoiceService.applyLateFee(eq(1L), any()))
                .thenThrow(new BusinessRuleException("Already paid"));
        mockMvc.perform(post(BASE + "/1/latefee").contentType(MediaType.APPLICATION_JSON).content(lateFeeBody()))
                .andExpect(status().isBadRequest());
    }

    // ── POST /{invoiceId}/latefee/waive ──────────────────────────────────

    @Test @DisplayName("POST /waive — 200 OK")
    void waive_200() throws Exception {
        when(invoiceService.waiveLateFee(eq(1L), any()))
                .thenReturn(new ApiResponse<>(200, "Fee waived", null));
        mockMvc.perform(post(BASE + "/1/latefee/waive").contentType(MediaType.APPLICATION_JSON).content(waiveBody()))
                .andExpect(status().isOk());
    }

    @Test @DisplayName("POST /waive — 400 on missing authorisedBy")
    void waive_400() throws Exception {
        mockMvc.perform(post(BASE + "/1/latefee/waive").contentType(MediaType.APPLICATION_JSON)
                .content("{\"waiverReason\":\"ok\"}")).andExpect(status().isBadRequest());
    }
}
