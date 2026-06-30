package com.teleConnect.billing.inovice.controller;

import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.DisputeResponse;
import com.teleConnect.billing.inovice.entity.enums.DisputeStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.GlobalExceptionHandler;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.service.BillingDisputeService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingDisputeController Tests")
class BillingDisputeControllerTest {

    @Mock BillingDisputeService disputeService;
    @InjectMocks BillingDisputeController controller;

    MockMvc mockMvc;
    private static final String BASE = "/teleConnect/billing/disputes";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private DisputeResponse sample() {
        return DisputeResponse.builder().disputeID(1L).invoiceID(1L)
                .disputedAmount(BigDecimal.valueOf(100)).disputeReason("Billing error")
                .status(DisputeStatus.Open).build();
    }

    private String createBody() {
        return "{\"invoiceId\":1,\"disputedAmount\":100,\"disputeReason\":\"Billing error\",\"description\":\"desc\"}";
    }

    // ── POST / createDispute ─────────────────────────────────────────────

    @Test @DisplayName("POST / — 201 Created")
    void create_201() throws Exception {
        when(disputeService.createDispute(any())).thenReturn(new ApiResponse<>(201, "Dispute created", null));
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isCreated());
    }

    @Test @DisplayName("POST / — response has message")
    void create_hasMessage() throws Exception {
        when(disputeService.createDispute(any())).thenReturn(new ApiResponse<>(201, "Dispute created", null));
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(jsonPath("$.message").value("Dispute created"));
    }

    @Test @DisplayName("POST / — 400 on missing disputeReason")
    void create_400_missingReason() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"invoiceId\":1,\"disputedAmount\":100}"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST / — 400 on missing invoiceId")
    void create_400_missingInvoiceId() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"disputedAmount\":100,\"disputeReason\":\"r\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST / — 404 when invoice not found")
    void create_404() throws Exception {
        when(disputeService.createDispute(any())).thenThrow(new ResourceNotFoundException("Invoice not found"));
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isNotFound());
    }

    @Test @DisplayName("POST / — 400 on BusinessRuleException")
    void create_businessRule() throws Exception {
        when(disputeService.createDispute(any())).thenThrow(new BusinessRuleException("Amount exceeds invoice"));
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount exceeds invoice"));
    }

    @Test @DisplayName("POST / — service not called on validation failure")
    void create_serviceNotCalled() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(disputeService, never()).createDispute(any());
    }

    // ── GET /{disputeId} ─────────────────────────────────────────────────

    @Test @DisplayName("GET /{disputeId} — 200 OK")
    void getById_200() throws Exception {
        when(disputeService.getDisputeById(1L)).thenReturn(new ApiResponse<>(200, "ok", sample()));
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isOk());
    }

    @Test @DisplayName("GET /{disputeId} — 404 when not found")
    void getById_404() throws Exception {
        when(disputeService.getDisputeById(99L)).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get(BASE + "/99")).andExpect(status().isNotFound());
    }

    @Test @DisplayName("GET /{disputeId} — response has data")
    void getById_hasData() throws Exception {
        when(disputeService.getDisputeById(1L)).thenReturn(new ApiResponse<>(200, "ok", sample()));
        mockMvc.perform(get(BASE + "/1")).andExpect(jsonPath("$.data").exists());
    }

    @Test @DisplayName("GET /{disputeId} — 500 on unexpected error")
    void getById_500() throws Exception {
        when(disputeService.getDisputeById(1L)).thenThrow(new RuntimeException("boom"));
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isInternalServerError());
    }

    // ── GET /account/{accountId} ─────────────────────────────────────────

    @Test @DisplayName("GET /account/{accountId} — 200 OK")
    void getByAccount_200() throws Exception {
        when(disputeService.getDisputesByAccount(eq(1001L), isNull(), any(Pageable.class)))
                .thenReturn(new ApiResponse<>(200, "ok", new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)));
        mockMvc.perform(get(BASE + "/account/1001")).andExpect(status().isOk());
    }

    @Test @DisplayName("GET /account/{accountId} — 500 on error")
    void getByAccount_500() throws Exception {
        when(disputeService.getDisputesByAccount(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("boom"));
        mockMvc.perform(get(BASE + "/account/1001")).andExpect(status().isInternalServerError());
    }
}
