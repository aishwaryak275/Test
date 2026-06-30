package com.teleConnect.service;

import com.teleConnect.dto.DisputeDTO;
import com.teleConnect.entity.Dispute;
import com.teleConnect.repository.DisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DisputeService {
    private final DisputeRepository disputeRepository;

    public DisputeService(DisputeRepository disputeRepository) {
        this.disputeRepository = disputeRepository;
    }

    @Transactional
    public Dispute createDispute(DisputeDTO dto) {
        Dispute d = new Dispute();
        d.setInvoiceID(dto.invoiceId);
        d.setSubscriberID(dto.subscriberId);
        d.setDisputedAmount(dto.disputedAmount);
        d.setRaisedDate(LocalDateTime.now());
        d.setStatus("OPEN");
        return disputeRepository.save(d);
    }
}
