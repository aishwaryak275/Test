package com.teleconnect.fault.controller;

import com.teleconnect.fault.dto.request.FaultTicketRequest;
import com.teleconnect.fault.dto.response.FaultTicketResponse;
import com.teleconnect.fault.dto.response.MessageResponse;
import com.teleconnect.fault.service.FaultTicketService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/fault")
public class FaultTicketController {

    private final FaultTicketService ticketService;
    private final AuditClient auditClient;

    public FaultTicketController(FaultTicketService ticketService, AuditClient auditClient) {
        this.ticketService = ticketService;
        this.auditClient = auditClient;
    }

    // POST /teleConnect/fault/createTickets
    @PostMapping("/createTickets")
    public ResponseEntity<MessageResponse> createTicket(
            @Valid @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        MessageResponse result = ticketService.createTicket(req);
        auditClient.record(AuditAction.CREATE_FAULT_TICKET, AuditModule.FAULT, httpReq);
        return ResponseEntity.status(201).body(result);
    }

    // GET /teleConnect/fault/getAllTickets
    @GetMapping("/getAllTickets")
    public ResponseEntity<List<FaultTicketResponse>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    // GET /teleConnect/fault/getTickets/{ticketId}
    @GetMapping("/getTickets/{ticketId}")
    public ResponseEntity<FaultTicketResponse> getTicketById(
            @PathVariable Integer ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    // PUT /teleConnect/fault/assignTickets/{ticketId}
    @PutMapping("/assignTickets/{ticketId}")
    public ResponseEntity<MessageResponse> assignTicket(
            @PathVariable Integer ticketId,
            @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        MessageResponse result = ticketService.assignTicket(ticketId, req);
        auditClient.record(AuditAction.ASSIGN_FAULT_TICKET, AuditModule.FAULT, httpReq);
        return ResponseEntity.ok(result);
    }

    // PUT /teleConnect/fault/updateTickets/{ticketId}
    @PutMapping("/updateTickets/{ticketId}")
    public ResponseEntity<MessageResponse> updateTicket(
            @PathVariable Integer ticketId,
            @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        MessageResponse result = ticketService.updateTicket(ticketId, req);
        auditClient.record(AuditAction.UPDATE_FAULT_TICKET, AuditModule.FAULT, httpReq);
        return ResponseEntity.ok(result);
    }

    // PUT /teleConnect/fault/resolveTickets/{ticketId}
    @PutMapping("/resolveTickets/{ticketId}")
    public ResponseEntity<MessageResponse> resolveTicket(
            @PathVariable Integer ticketId,
            @RequestBody FaultTicketRequest req,
            HttpServletRequest httpReq) {
        MessageResponse result = ticketService.resolveTicket(ticketId, req);
        auditClient.record(AuditAction.RESOLVE_FAULT_TICKET, AuditModule.FAULT, httpReq);
        return ResponseEntity.ok(result);
    }
}
