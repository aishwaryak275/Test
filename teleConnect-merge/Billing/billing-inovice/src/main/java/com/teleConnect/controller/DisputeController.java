package com.teleConnect.controller;

import com.teleConnect.dto.DisputeDTO;
import com.teleConnect.entity.Dispute;
import com.teleConnect.service.DisputeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teleConnect/billing/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DisputeDTO dto) {
        Dispute d = disputeService.createDispute(dto);
        return ResponseEntity.ok(d);
    }
}
