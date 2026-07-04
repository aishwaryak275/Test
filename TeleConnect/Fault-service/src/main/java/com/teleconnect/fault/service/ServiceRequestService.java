package com.teleconnect.fault.service;

import com.teleconnect.fault.dto.request.ServiceRequestRequest;
import com.teleconnect.fault.dto.response.*;
import com.teleconnect.fault.entity.ServiceRequest;
import com.teleconnect.fault.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepo;

    ServiceRequestService(ServiceRequestRepository requestRepo) {
        this.requestRepo = requestRepo;
    }

    // Convert Entity to Response DTO
    private ServiceRequestResponse toDTO(ServiceRequest r) {
        ServiceRequestResponse dto = new ServiceRequestResponse();
        dto.setRequestId(r.getRequestId());
        dto.setAccountId(r.getAccountId());
        dto.setLineId(r.getLineId());
        dto.setRequestType(r.getRequestType().name());
        dto.setRequestedBy(r.getRequestedBy());
        dto.setRaisedDate(r.getRaisedDate());
        dto.setStatus(r.getStatus().name());
        return dto;
    }

    // POST — create new service request
    public MessageResponse createRequest(ServiceRequestRequest req) {
        try {
            ServiceRequest.RequestType.valueOf(req.getRequestType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                "requestType must be PlanChange, SIMReplacement, PortingRequest, or AccountUpdate");
        }
        ServiceRequest sr = new ServiceRequest();
        sr.setAccountId(req.getAccountId());
        sr.setLineId(req.getLineId());
        sr.setRequestType(ServiceRequest.RequestType.valueOf(req.getRequestType()));
        sr.setRequestedBy(req.getRequestedBy());
        sr.setRaisedDate(req.getRaisedDate());
        sr.setStatus(ServiceRequest.RequestStatus.O);
        requestRepo.save(sr);
        return new MessageResponse("Service request created successfully");
    }

    // GET all
    public List<ServiceRequestResponse> getAllRequests() {
        return requestRepo.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    // GET by ID
    public ServiceRequestResponse getRequestById(Integer requestId) {
        ServiceRequest sr = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException(
                        "Service request with requestId " + requestId + " not found"));
        return toDTO(sr);
    }

    // PUT — update status
    public MessageResponse updateRequest(Integer requestId, ServiceRequestRequest req) {
        ServiceRequest sr = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException(
                        "Service request with requestId " + requestId + " not found"));
        if (req.getStatus() != null) {
            try {
                sr.setStatus(ServiceRequest.RequestStatus.valueOf(req.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("status must be O, P, C, or X");
            }
        }
        requestRepo.save(sr);
        return new MessageResponse("Service request updated successfully");
    }

    // PUT — cancel (only when Open)
    public MessageResponse cancelRequest(Integer requestId) {
        ServiceRequest sr = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException(
                        "Service request with requestId " + requestId + " not found"));
        if (sr.getStatus() != ServiceRequest.RequestStatus.O) {
            throw new RuntimeException("Only Open requests can be cancelled");
        }
        sr.setStatus(ServiceRequest.RequestStatus.X);
        requestRepo.save(sr);
        return new MessageResponse("Service request cancelled successfully");
    }
}
