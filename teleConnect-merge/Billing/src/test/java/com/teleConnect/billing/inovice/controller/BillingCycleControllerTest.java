package com.teleConnect.billing.inovice.controller;

import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.BillingCycleResponse;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.GlobalExceptionHandler;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.service.BillingCycleService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingCycleController Tests")
class BillingCycleControllerTest {

    @Mock BillingCycleService cycleService;
    @InjectMocks BillingCycleController controller;

    MockMvc mockMvc;
    private static final String BASE = "/teleConnect/billing/cycles";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private BillingCycleResponse sample() {
        return BillingCycleResponse.builder().cycleID(1L).accountID(1001L)
                .cycleStart(LocalDate.of(2026, 5, 1)).cycleEnd(LocalDate.of(2026, 5, 31))
                .status(CycleStatus.Open).build();
    }

    // ── POST /generate ───────────────────────────────────────────────────

    @Test @DisplayName("POST /generate — 200 OK")
    void generate_200() throws Exception {
        when(cycleService.generateInvoices(any())).thenReturn(new ApiResponse<>(200, "ok", null));
        mockMvc.perform(post(BASE + "/generate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cycleDate\":\"2026-05-31\",\"planCharges\":500,\"excessCharges\":200,\"addOnCharges\":100,\"taxes\":149.32}")).andExpect(status().isOk());
    }

    @Test @DisplayName("POST /generate — 400 on missing cycleDate")
    void generate_400() throws Exception {
        mockMvc.perform(post(BASE + "/generate").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST /generate — 400 on BusinessRuleException")
    void generate_businessRule() throws Exception {
        when(cycleService.generateInvoices(any())).thenThrow(new BusinessRuleException("No eligible cycles"));
        mockMvc.perform(post(BASE + "/generate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cycleDate\":\"2026-05-31\",\"planCharges\":500,\"excessCharges\":200,\"addOnCharges\":100,\"taxes\":149.32}")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("POST /generate — 500 on unexpected error")
    void generate_500() throws Exception {
        when(cycleService.generateInvoices(any())).thenThrow(new RuntimeException("boom"));
        mockMvc.perform(post(BASE + "/generate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"cycleDate\":\"2026-06-30\",\"planCharges\":500,\"excessCharges\":200,\"addOnCharges\":100,\"taxes\":149.32}")).andExpect(status().isInternalServerError());
    }

    @Test @DisplayName("POST /generate — service not called on validation failure")
    void generate_serviceNotCalled() throws Exception {
        mockMvc.perform(post(BASE + "/generate").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        verify(cycleService, never()).generateInvoices(any());
    }

    // ── GET /{accountId} ─────────────────────────────────────────────────

    @Test @DisplayName("GET /{accountId} — 200 OK")
    void getByAccount_200() throws Exception {
        when(cycleService.getCyclesByAccount(eq(1001L), isNull(), any(Pageable.class)))
                .thenReturn(new ApiResponse<>(200, "ok", new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)));
        mockMvc.perform(get(BASE + "/1001")).andExpect(status().isOk());
    }

    @Test @DisplayName("GET /{accountId} — response has data")
    void getByAccount_hasData() throws Exception {
        when(cycleService.getCyclesByAccount(eq(1001L), isNull(), any(Pageable.class)))
                .thenReturn(new ApiResponse<>(200, "ok",
                        new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1)));
        mockMvc.perform(get(BASE + "/1001")).andExpect(jsonPath("$.data").exists());
    }

    @Test @DisplayName("GET /{accountId} — 500 on error")
    void getByAccount_500() throws Exception {
        when(cycleService.getCyclesByAccount(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("boom"));
        mockMvc.perform(get(BASE + "/1001")).andExpect(status().isInternalServerError());
    }

    // ── GET /detail/{cycleId} ────────────────────────────────────────────

    @Test @DisplayName("GET /detail/{cycleId} — 200 OK")
    void getById_200() throws Exception {
        when(cycleService.getCycleById(1L)).thenReturn(new ApiResponse<>(200, "ok", sample()));
        mockMvc.perform(get(BASE + "/detail/1")).andExpect(status().isOk());
    }

    @Test @DisplayName("GET /detail/{cycleId} — 404 when not found")
    void getById_404() throws Exception {
        when(cycleService.getCycleById(99L)).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(get(BASE + "/detail/99")).andExpect(status().isNotFound());
    }

    @Test @DisplayName("GET /detail/{cycleId} — response has data")
    void getById_hasData() throws Exception {
        when(cycleService.getCycleById(1L)).thenReturn(new ApiResponse<>(200, "ok", sample()));
        mockMvc.perform(get(BASE + "/detail/1")).andExpect(jsonPath("$.data").exists());
    }

    // ── PUT /{cycleId}/close ──────────────────────────────────────────────

    @Test @DisplayName("PUT /{cycleId}/close — 200 OK")
    void close_200() throws Exception {
        when(cycleService.closeCycle(1L)).thenReturn(new ApiResponse<>(200, "Cycle closed", null));
        mockMvc.perform(put(BASE + "/1/close")).andExpect(status().isOk());
    }

    @Test @DisplayName("PUT /{cycleId}/close — 404 when not found")
    void close_404() throws Exception {
        when(cycleService.closeCycle(99L)).thenThrow(new ResourceNotFoundException("Not found"));
        mockMvc.perform(put(BASE + "/99/close")).andExpect(status().isNotFound());
    }

    @Test @DisplayName("PUT /{cycleId}/close — 400 on BusinessRuleException")
    void close_businessRule() throws Exception {
        when(cycleService.closeCycle(1L)).thenThrow(new BusinessRuleException("Already closed"));
        mockMvc.perform(put(BASE + "/1/close")).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("PUT /{cycleId}/close — 500 on unexpected error")
    void close_500() throws Exception {
        when(cycleService.closeCycle(1L)).thenThrow(new RuntimeException("boom"));
        mockMvc.perform(put(BASE + "/1/close")).andExpect(status().isInternalServerError());
    }
}
