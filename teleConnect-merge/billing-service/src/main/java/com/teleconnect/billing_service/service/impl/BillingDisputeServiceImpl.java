package com.teleconnect.billing_service.service.impl;

import com.teleconnect.billing_service.dto.request.DisputeRequest;
import com.teleconnect.billing_service.dto.request.DisputeResolveRequest;
import com.teleconnect.billing_service.dto.request.DisputeReviewRequest;
import com.teleconnect.billing_service.dto.response.DisputeResponse;
import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.DisputeStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingDisputeRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.BillingDisputeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillingDisputeServiceImpl implements BillingDisputeService {

    private final BillingDisputeRepository disputeRepository;
    private final InvoiceRepository invoiceRepository;

    public BillingDisputeServiceImpl(BillingDisputeRepository disputeRepository, InvoiceRepository invoiceRepository) {
        this.disputeRepository = disputeRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public DisputeResponse raiseDispute(DisputeRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invoice not found: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Cannot raise a dispute on an already paid invoice");
        }
        if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
            throw new BillingException("A dispute is already open for this invoice");
        }
        if (request.getDisputedAmount().compareTo(invoice.getTotalAmount()) > 0) {
            throw new BillingException(
                    "Disputed amount cannot exceed the invoice total of " + invoice.getTotalAmount());
        }

        // subscriberId is optional — fall back to invoice's accountId
        Long subscriberId = (request.getSubscriberId() != null)
                ? request.getSubscriberId()
                : invoice.getAccountId();

        BillingDispute dispute = BillingDispute.builder()
                .invoiceId(request.getInvoiceId())
                .subscriberId(subscriberId)
                .disputeReason(request.getDisputeReason())
                .description(request.getDescription())
                .disputedAmount(request.getDisputedAmount())
                .raisedDate(LocalDate.now())
                .status(DisputeStatus.OPEN)
                .build();

        invoice.setStatus(InvoiceStatus.DISPUTED);
        invoiceRepository.save(invoice);

        return toResponse(disputeRepository.save(dispute));
    }

    @Override
    public DisputeResponse getDisputeById(Long disputeId) {
        return toResponse(findById(disputeId));
    }

    @Override
    public List<DisputeResponse> getDisputesByInvoice(Long invoiceId) {
        return disputeRepository.findByInvoiceId(invoiceId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DisputeResponse> getDisputesBySubscriber(Long subscriberId) {
        return disputeRepository.findBySubscriberId(subscriberId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DisputeResponse> getDisputesByAccount(Long accountId, DisputeStatus status) {
        List<BillingDispute> disputes = disputeRepository.findBySubscriberId(accountId);
        if (status != null) {
            disputes = disputes.stream()
                    .filter(d -> d.getStatus() == status)
                    .collect(Collectors.toList());
        }
        return disputes.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DisputeResponse> getDisputesByStatus(DisputeStatus status) {
        return disputeRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DisputeResponse updateDisputeStatus(Long disputeId, DisputeStatus status) {
        BillingDispute dispute = findById(disputeId);

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new BillingException("Cannot update a dispute that is already " + dispute.getStatus());
        }

        dispute.setStatus(status);

        if (status == DisputeStatus.RESOLVED || status == DisputeStatus.REJECTED) {
            dispute.setResolvedDate(LocalDateTime.now());
            restoreInvoiceStatus(dispute.getInvoiceId());
        }

        return toResponse(disputeRepository.save(dispute));
    }

    @Override
    @Transactional
    public DisputeResponse reviewDispute(Long disputeId, DisputeReviewRequest request) {
        BillingDispute dispute = findById(disputeId);

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BillingException(
                    "Only OPEN disputes can be moved to Under Review. Current status: " + dispute.getStatus());
        }

        dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        dispute.setAssignedTo(request.getAssignedTo());
        dispute.setAcknowledgedDate(LocalDateTime.now());

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            dispute.setResolutionNotes(request.getNotes());
        }

        return toResponse(disputeRepository.save(dispute));
    }

    @Override
    @Transactional
    public DisputeResponse resolveDispute(Long disputeId, DisputeResolveRequest request) {
        BillingDispute dispute = findById(disputeId);

        if (dispute.getStatus() == DisputeStatus.RESOLVED || dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new BillingException("Dispute is already closed with status: " + dispute.getStatus());
        }

        boolean isResolved = "Resolved".equalsIgnoreCase(request.getResolution());
        DisputeStatus newStatus = isResolved ? DisputeStatus.RESOLVED : DisputeStatus.REJECTED;

        if (!isResolved && (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank())) {
            throw new BillingException("Resolution notes are mandatory when rejecting a dispute");
        }

        dispute.setStatus(newStatus);
        dispute.setResolvedDate(LocalDateTime.now());
        dispute.setResolutionNotes(request.getResolutionNotes());

        if (isResolved && request.getCreditAmount() != null) {
            dispute.setResolvedAmount(request.getCreditAmount());
        }

        restoreInvoiceStatus(dispute.getInvoiceId());

        return toResponse(disputeRepository.save(dispute));
    }

    private void restoreInvoiceStatus(Long invoiceId) {
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            InvoiceStatus restored = invoice.getDueDate().isBefore(LocalDate.now())
                    ? InvoiceStatus.OVERDUE
                    : InvoiceStatus.SENT;
            invoice.setStatus(restored);
            invoiceRepository.save(invoice);
        });
    }

    private BillingDispute findById(Long disputeId) {
        return disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dispute not found with ID: " + disputeId));
    }

    private DisputeResponse toResponse(BillingDispute dispute) {
        return DisputeResponse.builder()
                .disputeId(dispute.getDisputeId())
                .invoiceId(dispute.getInvoiceId())
                .subscriberId(dispute.getSubscriberId())
                .disputeReason(dispute.getDisputeReason())
                .description(dispute.getDescription())
                .disputedAmount(dispute.getDisputedAmount())
                .resolvedAmount(dispute.getResolvedAmount())
                .raisedDate(dispute.getRaisedDate())
                .acknowledgedDate(dispute.getAcknowledgedDate())
                .resolvedDate(dispute.getResolvedDate())
                .assignedTo(dispute.getAssignedTo())
                .resolutionNotes(dispute.getResolutionNotes())
                .status(dispute.getStatus())
                .build();
    }
}
