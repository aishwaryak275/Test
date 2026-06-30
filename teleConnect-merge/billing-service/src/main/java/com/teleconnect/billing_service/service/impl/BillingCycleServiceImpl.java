package com.teleconnect.billing_service.service.impl;

import com.teleconnect.billing_service.dto.request.BillingCycleRequest;
import com.teleconnect.billing_service.dto.request.CycleGenerationRequest;
import com.teleconnect.billing_service.dto.response.BatchGenerationResponse;
import com.teleconnect.billing_service.dto.response.BillingCycleResponse;
import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.entity.Invoice;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import com.teleconnect.billing_service.enums.InvoiceStatus;
import com.teleconnect.billing_service.exception.BillingException;
import com.teleconnect.billing_service.exception.ResourceNotFoundException;
import com.teleconnect.billing_service.repository.BillingCycleRepository;
import com.teleconnect.billing_service.repository.InvoiceRepository;
import com.teleconnect.billing_service.service.BillingCycleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillingCycleServiceImpl implements BillingCycleService {

    @Autowired
    private BillingCycleRepository billingCycleRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public BillingCycleResponse createBillingCycle(BillingCycleRequest request) {
        if (request.getCycleEnd().isBefore(request.getCycleStart())) {
            throw new BillingException("Cycle end date must be after cycle start date");
        }

        billingCycleRepository.findByAccountIdAndStatus(request.getAccountId(), BillingCycleStatus.OPEN)
                .ifPresent(c -> {
                    throw new BillingException(
                            "An open billing cycle already exists for account: " + request.getAccountId());
                });

        BillingCycle cycle = BillingCycle.builder()
                .accountId(request.getAccountId())
                .cycleStart(request.getCycleStart())
                .cycleEnd(request.getCycleEnd())
                .status(BillingCycleStatus.OPEN)
                .build();

        return toResponse(billingCycleRepository.save(cycle));
    }

    @Override
    @Transactional
    public BatchGenerationResponse generateInvoicesBatch(CycleGenerationRequest request) {
        List<BillingCycle> eligible = billingCycleRepository
                .findByStatusAndCycleEndLessThanEqual(BillingCycleStatus.OPEN, request.getCycleDate());

        int processed = 0;
        int generated = 0;
        int skipped = 0;
        int errors = 0;

        for (BillingCycle cycle : eligible) {
            processed++;
            try {
                boolean invoiceExists = invoiceRepository
                        .findByAccountIdAndCycleId(cycle.getAccountId(), cycle.getCycleId())
                        .isPresent();
                if (invoiceExists) {
                    skipped++;
                    continue;
                }

                if (!request.isDryRun()) {
                    // Charge components are zero-initialised here: plan and usage data live in
                    // the Plan/Usage modules (out of this service's Phase 1 scope) and are
                    // populated via the explicit generate-invoice endpoint or later integration.
                    Invoice invoice = Invoice.builder()
                            .accountId(cycle.getAccountId())
                            .cycleId(cycle.getCycleId())
                            .planCharges(BigDecimal.ZERO)
                            .excessCharges(BigDecimal.ZERO)
                            .addOnCharges(BigDecimal.ZERO)
                            .taxes(BigDecimal.ZERO)
                            .totalAmount(BigDecimal.ZERO)
                            .dueDate(cycle.getCycleEnd().plusDays(15))
                            .status(InvoiceStatus.GENERATED)
                            .build();
                    invoiceRepository.save(invoice);

                    cycle.setStatus(BillingCycleStatus.GENERATED);
                    cycle.setGeneratedDate(LocalDate.now());
                    billingCycleRepository.save(cycle);
                }
                generated++;
            } catch (Exception ex) {
                errors++;
            }
        }

        return new BatchGenerationResponse(
                processed, generated, skipped, errors, request.isDryRun(), LocalDateTime.now());
    }

    @Override
    public BillingCycleResponse getBillingCycleById(Long cycleId) {
        return toResponse(findById(cycleId));
    }

    @Override
    public List<BillingCycleResponse> getCyclesByAccount(Long accountId) {
        return billingCycleRepository.findByAccountId(accountId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public Page<BillingCycleResponse> getCyclesByAccount(Long accountId, BillingCycleStatus status, Pageable pageable) {
        Page<BillingCycle> page = (status == null)
                ? billingCycleRepository.findByAccountId(accountId, pageable)
                : billingCycleRepository.findByAccountIdAndStatus(accountId, status, pageable);
        return page.map(this::toResponse);
    }

    @Override
    public List<BillingCycleResponse> getCyclesByStatus(BillingCycleStatus status) {
        return billingCycleRepository.findByStatus(status)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BillingCycleResponse updateCycleStatus(Long cycleId, BillingCycleStatus status) {
        BillingCycle cycle = findById(cycleId);
        cycle.setStatus(status);
        if (status == BillingCycleStatus.GENERATED) {
            cycle.setGeneratedDate(LocalDate.now());
        }
        return toResponse(billingCycleRepository.save(cycle));
    }

    @Override
    @Transactional
    public BillingCycleResponse closeBillingCycle(Long cycleId) {
        BillingCycle cycle = findById(cycleId);
        if (cycle.getStatus() == BillingCycleStatus.CLOSED) {
            throw new BillingException("Billing cycle is already closed");
        }
        cycle.setStatus(BillingCycleStatus.CLOSED);
        return toResponse(billingCycleRepository.save(cycle));
    }

    private BillingCycle findById(Long cycleId) {
        return billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billing cycle not found with ID: " + cycleId));
    }

    private BillingCycleResponse toResponse(BillingCycle cycle) {
        return BillingCycleResponse.builder()
                .cycleId(cycle.getCycleId())
                .accountId(cycle.getAccountId())
                .cycleStart(cycle.getCycleStart())
                .cycleEnd(cycle.getCycleEnd())
                .generatedDate(cycle.getGeneratedDate())
                .status(cycle.getStatus())
                .createdAt(cycle.getCreatedAt())
                .updatedAt(cycle.getUpdatedAt())
                .build();
    }
}
