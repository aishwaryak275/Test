package com.teleConnect.billing.inovice.service;

import com.teleConnect.billing.inovice.dto.request.CreateDisputeRequest;
import com.teleConnect.billing.inovice.dto.response.ApiResponse;
import com.teleConnect.billing.inovice.dto.response.DisputeResponse;
import com.teleConnect.billing.inovice.entity.BillingDispute;
import com.teleConnect.billing.inovice.entity.Invoice;
import com.teleConnect.billing.inovice.entity.enums.DisputeStatus;
import com.teleConnect.billing.inovice.entity.enums.InvoiceStatus;
import com.teleConnect.billing.inovice.exception.BusinessRuleException;
import com.teleConnect.billing.inovice.exception.ResourceNotFoundException;
import com.teleConnect.billing.inovice.repository.BillingDisputeRepository;
import com.teleConnect.billing.inovice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingDisputeServiceImpl implements BillingDisputeService {

    private final BillingDisputeRepository disputeRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public ApiResponse<Void> createDispute(CreateDisputeRequest request) {

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + request.invoiceId()));

        // Business rule from spec: invoice must be Generated, Sent, or Overdue
        List<InvoiceStatus> allowedStatuses = List.of(
                InvoiceStatus.Generated, InvoiceStatus.Sent, InvoiceStatus.Overdue);

        if (!allowedStatuses.contains(invoice.getStatus())) {
            throw new BusinessRuleException(
                    "Dispute can only be raised on Generated, Sent, or Overdue invoices. Current status: "
                            + invoice.getStatus());
        }

        // Business rule: disputedAmount must be > 0 and <= totalAmount
        if (request.disputedAmount().compareTo(invoice.getTotalAmount()) > 0) {
            throw new BusinessRuleException(
                    "disputedAmount (" + request.disputedAmount()
                            + ") cannot exceed invoice totalAmount (" + invoice.getTotalAmount() + ")");
        }

        BillingDispute dispute = BillingDispute.builder()
                .invoice(invoice)
                .subscriberID(invoice.getAccountID())
                .disputeReason(request.disputeReason())
                .disputedAmount(request.disputedAmount())
                .description(request.description())
                .raisedDate(LocalDateTime.now())
                .status(DisputeStatus.Open)
                .build();
        disputeRepository.save(dispute);

        return ApiResponse.created("Billing dispute raised successfully");
    }

    @Override
    public ApiResponse<DisputeResponse> getDisputeById(Long disputeId) {
        BillingDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        return ApiResponse.success("Dispute retrieved", DisputeResponse.from(dispute));
    }

    @Override
    public ApiResponse<Page<DisputeResponse>> getDisputesByAccount(Long accountId, String status, Pageable pageable) {
        Page<BillingDispute> page;
        if (status != null && !status.isBlank()) {
            DisputeStatus disputeStatus = DisputeStatus.valueOf(status);
            page = disputeRepository.findByInvoice_AccountIDAndStatus(accountId, disputeStatus, pageable);
        } else {
            page = disputeRepository.findByInvoice_AccountID(accountId, pageable);
        }
        return ApiResponse.success("Disputes retrieved", page.map(DisputeResponse::from));
    }
}
