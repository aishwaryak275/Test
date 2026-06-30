package com.teleConnect.billing.inovice.controller;

import com.teleConnect.billing.inovice.dto.request.CreateBillingCycleRequest;
import com.teleConnect.billing.inovice.dto.request.GenerateCycleRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.BillingCycleResponse;
import com.teleConnect.billing.inovice.dto.response.MessageResponse;
import com.teleConnect.billing.inovice.service.BillingCycleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teleConnect/billing/cycles")
public class BillingCycleController {

    private final BillingCycleService cycleService;

    public BillingCycleController(BillingCycleService cycleService) {
        this.cycleService = cycleService;
    }

    /**
     * POST /teleConnect/billing/cycles
     * Creates a new billing cycle with Open status.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BillingCycleResponse>> createCycle(
            @Valid @RequestBody CreateBillingCycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cycleService.createCycle(request));
    }

    /**
     * POST /teleConnect/billing/cycles/generate
     * Triggers monthly batch invoice generation for all eligible Open cycles.
     */
    @PostMapping("/generate")
    public ResponseEntity<MessageResponse> generateInvoices(
            @Valid @RequestBody GenerateCycleRequest request) {
        return ResponseEntity.ok(new MessageResponse(cycleService.generateInvoices(request).getMessage()));
    }

    /**
     * GET /teleConnect/billing/cycles/{accountId}
     * Returns paginated billing cycles for a given account.
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Page<BillingCycleResponse>>> getCyclesByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cycleService.getCyclesByAccount(accountId, status, pageable));
    }

    /**
     * GET /teleConnect/billing/cycles/detail/{cycleId}
     * Returns full details of one billing cycle by its ID.
     * Note: using /detail/{cycleId} to avoid path conflict with /{accountId}.
     * In production, separate this into a different path or use query params.
     */
    @GetMapping("/detail/{cycleId}")
    public ResponseEntity<ApiResponse<BillingCycleResponse>> getCycleById(
            @PathVariable Long cycleId) {
        return ResponseEntity.ok(cycleService.getCycleById(cycleId));
    }

    /**
     * PUT /teleConnect/billing/cycles/{cycleId}/close
     * Manually closes a billing cycle. Irreversible.
     */
    @PutMapping("/{cycleId}/close")
    public ResponseEntity<ApiResponse<Void>> closeCycle(@PathVariable Long cycleId) {
        return ResponseEntity.ok(cycleService.closeCycle(cycleId));
    }
}
