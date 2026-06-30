package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.CreateBillingCycleRequest;
import com.teleConnect.billing.inovice.dto.request.GenerateCycleRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.BillingCycleResponse;
import com.teleConnect.billing.inovice.entity.BillingCycle;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.CycleStatus;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.DuplicateResourceException;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.repository.BillingCycleRepository;
import com.teleConnect.billing.inovice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingCycleServiceImpl implements BillingCycleService {

    private final BillingCycleRepository cycleRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public ApiResponse<BillingCycleResponse> createCycle(CreateBillingCycleRequest request) {
        if (request.cycleEnd().isBefore(request.cycleStart())) {
            throw new BusinessRuleException("cycleEnd must be after cycleStart");
        }

        BillingCycle cycle = BillingCycle.builder()
                .accountID(request.accountID())
                .cycleStart(request.cycleStart())
                .cycleEnd(request.cycleEnd())
                .status(CycleStatus.Open)
                .build();

        cycleRepository.save(cycle);
        return ApiResponse.success("Billing cycle created successfully", BillingCycleResponse.from(cycle));
    }

    @Override
    @Transactional
    public ApiResponse<Void> generateInvoices(GenerateCycleRequest request) {

        LocalDate cycleDate = LocalDate.parse(request.cycleDate());

        BigDecimal total = request.planCharges()
                .add(request.excessCharges())
                .add(request.addOnCharges())
                .add(request.taxes());

        // Find all Open cycles where cycleEnd <= given date
        List<BillingCycle> eligibleCycles = cycleRepository
                .findByStatusAndCycleEndLessThanEqual(CycleStatus.Open, cycleDate);

        if (eligibleCycles.isEmpty()) {
            throw new BusinessRuleException("No eligible Open cycles found for the given date");
        }

        for (BillingCycle cycle : eligibleCycles) {

            // 409 Conflict: invoice already exists for this cycle
            if (invoiceRepository.findByCycle_CycleID(cycle.getCycleID()).isPresent()) {
                throw new DuplicateResourceException(
                        "Invoice already exists for cycleID: " + cycle.getCycleID());
            }

            Invoice invoice = Invoice.builder()
                    .accountID(cycle.getAccountID())
                    .cycle(cycle)
                    .planCharges(request.planCharges())
                    .excessCharges(request.excessCharges())
                    .addOnCharges(request.addOnCharges())
                    .taxes(request.taxes())
                    .totalAmount(total)
                    .dueDate(cycleDate.plusDays(22))
                    .status(InvoiceStatus.Generated)
                    .build();
            invoiceRepository.save(invoice);

            // Move cycle to Generated
            cycle.setStatus(CycleStatus.Generated);
            cycle.setGeneratedDate(LocalDateTime.now());
            cycleRepository.save(cycle);
        }

        return ApiResponse.success("Invoice generation completed successfully");
    }

    @Override
    public ApiResponse<Page<BillingCycleResponse>> getCyclesByAccount(Long accountId, String status, Pageable pageable) {
        Page<BillingCycle> page;
        if (status != null && !status.isBlank()) {
            CycleStatus cycleStatus = CycleStatus.valueOf(status);
            page = cycleRepository.findByAccountIDAndStatus(accountId, cycleStatus, pageable);
        } else {
            page = cycleRepository.findByAccountID(accountId, pageable);
        }
        return ApiResponse.success("Billing cycles retrieved", page.map(BillingCycleResponse::from));
    }

    @Override
    public ApiResponse<BillingCycleResponse> getCycleById(Long cycleId) {
        BillingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing cycle not found: " + cycleId));
        return ApiResponse.success("Billing cycle retrieved", BillingCycleResponse.from(cycle));
    }

    @Override
    @Transactional
    public ApiResponse<Void> closeCycle(Long cycleId) {
        BillingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing cycle not found: " + cycleId));

        if (cycle.getStatus() != CycleStatus.Generated) {
            throw new BusinessRuleException(
                    "Only Generated cycles can be closed. Current status: " + cycle.getStatus());
        }

        cycle.setStatus(CycleStatus.Closed);
        cycleRepository.save(cycle);
        return ApiResponse.success("Billing cycle closed successfully");
    }
}
