package com.teleconnect.subscriber.controller;

import com.teleconnect.subscriber.dto.request.*;
import com.teleconnect.subscriber.dto.response.*;
import com.teleconnect.subscriber.service.SimLineService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/teleConnect/api/subscribers")
public class SimLineController {

    private final SimLineService simLineService;
    private final AuditClient auditClient;

    public SimLineController(SimLineService simLineService, AuditClient auditClient) {
        this.simLineService = simLineService;
        this.auditClient = auditClient;
    }

    @PostMapping("/{accountId}/simLines")
    @PreAuthorize("hasAnyAuthority('CREATE_USER','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> createSimLine(
            @PathVariable Integer accountId,
            @Valid @RequestBody CreateSimLineRequest req,
            HttpServletRequest httpReq) {
        var result = simLineService.createSimLine(accountId, req);
        auditClient.record(AuditAction.CREATE_SIM_LINE, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.status(201)
            .body(result);
    }

    @GetMapping("/{accountId}/simLines")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER','VIEW_OWN_PLAN')")
    public ResponseEntity<List<SimLineResponseDTO>> getSimLines(
            @PathVariable Integer accountId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(
            simLineService.getSimLinesByAccount(accountId, status));
    }

    @GetMapping("/{accountId}/simLines/{lineId}")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER','VIEW_OWN_PLAN')")
    public ResponseEntity<SimLineResponseDTO> getSimLine(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId) {
        return ResponseEntity.ok(
            simLineService.getSimLineById(accountId, lineId));
    }

    @GetMapping("/sim-lines/lookup")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<SimLineResponseDTO> lookupByMsisdn(
            @RequestParam String msisdn) {
        return ResponseEntity.ok(simLineService.lookupByMsisdn(msisdn));
    }

    @PutMapping("/{accountId}/simLines/{lineId}/status")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> updateSimStatus(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            @Valid @RequestBody UpdateSimStatusRequest req,
            HttpServletRequest httpReq) {
        var result = simLineService.updateSimStatus(accountId, lineId, req);
        auditClient.record(AuditAction.UPDATE_SIM_STATUS, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{accountId}/simLines/{lineId}/replace")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<SimLineResponseDTO> replaceSim(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            @Valid @RequestBody ReplaceSimRequest req,
            HttpServletRequest httpReq) {
        var result = simLineService.replaceSim(accountId, lineId, req);
        auditClient.record(AuditAction.REPLACE_SIM, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{accountId}/simLines/{lineId}/service-type")
    @PreAuthorize("hasAnyAuthority('VIEW_ALL_USERS','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> updateServiceType(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            @Valid @RequestBody UpdateServiceTypeRequest req,
            HttpServletRequest httpReq) {
        var result = simLineService.updateServiceType(accountId, lineId, req);
        auditClient.record(AuditAction.UPDATE_SIM_SERVICE_TYPE, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{accountId}/simLines/{lineId}")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<MessageDTO> deleteSimLine(
            @PathVariable Integer accountId,
            @PathVariable Integer lineId,
            HttpServletRequest httpReq) {
        var result = simLineService.deleteSimLine(accountId, lineId);
        auditClient.record(AuditAction.DELETE_SIM_LINE, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.ok(result);
    }
}