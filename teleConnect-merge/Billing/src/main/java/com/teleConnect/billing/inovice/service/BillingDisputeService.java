package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.CreateDisputeRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.DisputeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BillingDisputeService {

    // POST /disputes
    ApiResponse<Void> createDispute(CreateDisputeRequest request);

    // GET /disputes/{disputeId}
    ApiResponse<DisputeResponse> getDisputeById(Long disputeId);

    // GET /disputes/account/{accountId}
    ApiResponse<Page<DisputeResponse>> getDisputesByAccount(Long accountId, String status, Pageable pageable);
}
