package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.CreateBillingCycleRequest;
import com.teleConnect.billing.inovice.dto.request.GenerateCycleRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.BillingCycleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BillingCycleService {

    // POST /cycles
    ApiResponse<BillingCycleResponse> createCycle(CreateBillingCycleRequest request);

    // POST /cycles/generate
    ApiResponse<Void> generateInvoices(GenerateCycleRequest request);

    // GET /cycles/{accountId}
    ApiResponse<Page<BillingCycleResponse>> getCyclesByAccount(Long accountId, String status, Pageable pageable);

    // GET /cycles/{cycleId}  (by cycleId directly)
    ApiResponse<BillingCycleResponse> getCycleById(Long cycleId);

    // PUT /cycles/{cycleId}/close
    ApiResponse<Void> closeCycle(Long cycleId);
}
