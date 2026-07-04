package com.teleconnect.fault.service;

import com.teleconnect.fault.dto.request.FaultTicketRequest;
import com.teleconnect.fault.dto.response.*;
import com.teleconnect.fault.entity.FaultTicket;
import com.teleconnect.fault.repository.FaultTicketRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FaultTicketService {

    private final FaultTicketRepository ticketRepo;

    FaultTicketService(FaultTicketRepository ticketRepo) {
        this.ticketRepo = ticketRepo;
    }

    // Convert Entity to Response DTO
    private FaultTicketResponse toDTO(FaultTicket t) {
        FaultTicketResponse dto = new FaultTicketResponse();
        dto.setTicketId(t.getTicketId());
        dto.setAccountId(t.getAccountId());
        dto.setLineId(t.getLineId());
        dto.setFaultType(t.getFaultType().name());
        dto.setDescription(t.getDescription());
        dto.setPriority(t.getPriority().name());
        dto.setRaisedDate(t.getRaisedDate());
        dto.setResolvedDate(t.getResolvedDate());
        dto.setAssignedToId(t.getAssignedToId());
        dto.setStatus(t.getStatus().name());
        return dto;
    }

    // POST — create fault ticket
    public MessageResponse createTicket(FaultTicketRequest req) {
        try {
            FaultTicket.FaultType.valueOf(req.getFaultType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                "faultType must be NoCoverage, CallDrops, SlowData, BillingIssue, or Activation");
        }
        FaultTicket ticket = new FaultTicket();
        ticket.setAccountId(req.getAccountId());
        ticket.setLineId(req.getLineId());
        ticket.setFaultType(FaultTicket.FaultType.valueOf(req.getFaultType()));
        ticket.setDescription(req.getDescription());
        // priority defaults to M if not provided
        if (req.getPriority() != null) {
            try {
                ticket.setPriority(FaultTicket.Priority.valueOf(req.getPriority()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("priority must be L, M, H, or C");
            }
        } else {
            ticket.setPriority(FaultTicket.Priority.M);
        }
        ticket.setRaisedDate(req.getRaisedDate());
        ticket.setAssignedToId(req.getAssignedToId());
        ticket.setStatus(FaultTicket.TicketStatus.O);
        ticketRepo.save(ticket);
        return new MessageResponse("Fault ticket created successfully");
    }

    // GET all
    public List<FaultTicketResponse> getAllTickets() {
        return ticketRepo.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    // GET by ID
    public FaultTicketResponse getTicketById(Integer ticketId) {
        FaultTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException(
                        "Fault ticket with ticketId " + ticketId + " not found"));
        return toDTO(ticket);
    }

    // PUT — assign to engineer
    public MessageResponse assignTicket(Integer ticketId, FaultTicketRequest req) {
        FaultTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException(
                        "Fault ticket with ticketId " + ticketId + " not found"));
        if (req.getAssignedToId() == null) {
            throw new RuntimeException("assignedToId is required");
        }
        ticket.setAssignedToId(req.getAssignedToId());
        ticketRepo.save(ticket);
        return new MessageResponse("Fault ticket assigned successfully");
    }

    // PUT — update status
    public MessageResponse updateTicket(Integer ticketId, FaultTicketRequest req) {
        FaultTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException(
                        "Fault ticket with ticketId " + ticketId + " not found"));
        if (req.getStatus() != null) {
            try {
                ticket.setStatus(FaultTicket.TicketStatus.valueOf(req.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("status must be O, P, R, C, or E");
            }
        }
        ticketRepo.save(ticket);
        return new MessageResponse("Fault ticket updated successfully");
    }

    // PUT — resolve ticket
    public MessageResponse resolveTicket(Integer ticketId, FaultTicketRequest req) {
        FaultTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException(
                        "Fault ticket with ticketId " + ticketId + " not found"));
        if (req.getResolvedDate() == null) {
            throw new RuntimeException("resolvedDate is required");
        }
        if (req.getResolvedDate().isBefore(ticket.getRaisedDate())) {
            throw new RuntimeException("resolvedDate cannot be before raisedDate");
        }
        ticket.setResolvedDate(req.getResolvedDate());
        ticket.setStatus(FaultTicket.TicketStatus.R);
        ticketRepo.save(ticket);
        return new MessageResponse("Fault ticket resolved successfully");
    }
}
