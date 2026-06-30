package com.teleConnect.billing.inovice.controller;

import com.teleConnect.billing.inovice.dto.request.CreateDisputeRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.DisputeResponse;
import com.teleConnect.billing.inovice.dto.response.MessageResponse;
import com.teleConnect.billing.inovice.service.BillingDisputeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teleConnect/billing/disputes")
public class BillingDisputeController {

    private final BillingDisputeService disputeService;

    public BillingDisputeController(BillingDisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * POST /teleConnect/billing/disputes
     * Creates a new billing dispute. Returns 201 Created.
     */
    @PostMapping
    public ResponseEntity<MessageResponse> createDispute(
            @Valid @RequestBody CreateDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse(disputeService.createDispute(request).getMessage()));
    }

    /**
     * GET /teleConnect/billing/disputes/{disputeId}
     * Returns full dispute details.
     */
    @GetMapping("/{disputeId}")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDisputeById(
            @PathVariable Long disputeId) {
        return ResponseEntity.ok(disputeService.getDisputeById(disputeId));
    }

    /**
     * GET /teleConnect/billing/disputes/account/{accountId}
     * Returns all disputes for a given account with pagination and status filtering.
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse<Page<DisputeResponse>>> getDisputesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(disputeService.getDisputesByAccount(accountId, status, pageable));
    }
}
