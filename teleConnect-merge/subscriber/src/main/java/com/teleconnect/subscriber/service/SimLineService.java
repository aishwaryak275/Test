package com.teleconnect.subscriber.service;

import com.teleconnect.subscriber.dto.request.*;
import com.teleconnect.subscriber.dto.response.*;
import com.teleconnect.subscriber.entity.SimLine;
import com.teleconnect.subscriber.repository.SimLineRepository;
import com.teleconnect.subscriber.repository.SubscriberAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SimLineService {

    @Autowired private SimLineRepository simLineRepo;
    @Autowired private SubscriberAccountRepository accountRepo;

    private SimLineResponseDTO toDTO(SimLine sl) {
        SimLineResponseDTO dto = new SimLineResponseDTO();
        dto.setLineId(sl.getLineId());
        dto.setAccountId(sl.getAccountId());
        dto.setMsisdn(sl.getMsisdn());
        dto.setIccid(sl.getIccid());
        dto.setActivationDate(sl.getActivationDate());
        dto.setServiceType(sl.getServiceType().name());
        dto.setStatus(sl.getStatus().name());
        dto.setCreatedAt(sl.getCreatedAt());
        dto.setUpdatedAt(sl.getUpdatedAt());
        return dto;
    }

    public MessageDTO createSimLine(Integer accountId,
                                    CreateSimLineRequest req) {
        accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        if (simLineRepo.existsByMsisdn(req.getMsisdn()))
            throw new RuntimeException("MSISDN already in use: " + req.getMsisdn());
        if (simLineRepo.existsByIccid(req.getIccid()))
            throw new RuntimeException("ICCID already in use: " + req.getIccid());
        SimLine simLine = new SimLine();
        simLine.setAccountId(accountId);
        simLine.setMsisdn(req.getMsisdn());
        simLine.setIccid(req.getIccid());
        simLine.setServiceType(SimLine.ServiceType.valueOf(req.getServiceType()));
        simLineRepo.save(simLine);
        return new MessageDTO("SIM line activated successfully");
    }

    public List<SimLineResponseDTO> getSimLinesByAccount(
            Integer accountId, String status) {
        accountRepo.findById(accountId)
            .orElseThrow(() -> new RuntimeException(
                "Account not found: " + accountId));
        List<SimLine> lines = status != null
            ? simLineRepo.findByAccountIdAndStatus(
                accountId, SimLine.SimStatus.valueOf(status))
            : simLineRepo.findByAccountId(accountId);
        return lines.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public SimLineResponseDTO getSimLineById(Integer accountId, Integer lineId) {
        SimLine simLine = simLineRepo.findById(lineId)
            .orElseThrow(() -> new RuntimeException(
                "SIM line not found: " + lineId));
        if (!simLine.getAccountId().equals(accountId))
            throw new RuntimeException(
                "SIM line " + lineId + " does not belong to account " + accountId);
        return toDTO(simLine);
    }

    public SimLineResponseDTO lookupByMsisdn(String msisdn) {
        SimLine simLine = simLineRepo.findByMsisdn(msisdn)
            .orElseThrow(() -> new RuntimeException(
                "No SIM line found for MSISDN: " + msisdn));
        return toDTO(simLine);
    }

    public MessageDTO updateSimStatus(Integer accountId, Integer lineId,
                                      UpdateSimStatusRequest req) {
        SimLine simLine = simLineRepo.findById(lineId)
            .orElseThrow(() -> new RuntimeException(
                "SIM line not found: " + lineId));
        if (!simLine.getAccountId().equals(accountId))
            throw new RuntimeException(
                "SIM line " + lineId + " does not belong to account " + accountId);
        simLine.setStatus(SimLine.SimStatus.valueOf(req.getStatus()));
        simLineRepo.save(simLine);
        return new MessageDTO("SIM line status updated to " + req.getStatus());
    }

    public SimLineResponseDTO replaceSim(Integer accountId, Integer lineId,
                                         ReplaceSimRequest req) {
        SimLine simLine = simLineRepo.findById(lineId)
            .orElseThrow(() -> new RuntimeException(
                "SIM line not found: " + lineId));
        if (!simLine.getAccountId().equals(accountId))
            throw new RuntimeException(
                "SIM line " + lineId + " does not belong to account " + accountId);
        if (simLineRepo.existsByIccid(req.getNewIccid()))
            throw new RuntimeException("ICCID already in use: " + req.getNewIccid());
        simLine.setIccid(req.getNewIccid());
        SimLine saved = simLineRepo.save(simLine);
        return toDTO(saved);
    }

    public MessageDTO updateServiceType(Integer accountId, Integer lineId,
                                        UpdateServiceTypeRequest req) {
        SimLine simLine = simLineRepo.findById(lineId)
            .orElseThrow(() -> new RuntimeException(
                "SIM line not found: " + lineId));
        if (!simLine.getAccountId().equals(accountId))
            throw new RuntimeException(
                "SIM line " + lineId + " does not belong to account " + accountId);
        simLine.setServiceType(SimLine.ServiceType.valueOf(req.getServiceType()));
        simLineRepo.save(simLine);
        return new MessageDTO("Service type updated to " + req.getServiceType());
    }

    public MessageDTO deleteSimLine(Integer accountId, Integer lineId) {
        SimLine simLine = simLineRepo.findById(lineId)
            .orElseThrow(() -> new RuntimeException(
                "SIM line not found: " + lineId));
        if (!simLine.getAccountId().equals(accountId))
            throw new RuntimeException(
                "SIM line " + lineId + " does not belong to account " + accountId);
        simLine.setStatus(SimLine.SimStatus.Deactivated);
        simLineRepo.save(simLine);
        return new MessageDTO("SIM line " + lineId + " deactivated successfully.");
    }
}