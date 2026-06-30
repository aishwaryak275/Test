package com.teleConnect.controller;

import com.teleConnect.dto.BillingCycleDTO;
import com.teleConnect.entity.BillingCycle;
import com.teleConnect.service.BillingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teleConnect/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/cycles/generate")
    public ResponseEntity<?> generate(@RequestBody BillingCycleDTO dto) {
        BillingCycle bc = billingService.generateCycle(dto);
        return ResponseEntity.ok(bc);
    }
}
