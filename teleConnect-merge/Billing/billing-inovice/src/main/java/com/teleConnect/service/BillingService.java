package com.teleConnect.service;

import com.teleConnect.dto.BillingCycleDTO;
import com.teleConnect.entity.BillingCycle;
import com.teleConnect.entity.Invoice;
import com.teleConnect.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class BillingService {

    private final InvoiceRepository invoiceRepository;

    public BillingService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public BillingCycle generateCycle(BillingCycleDTO dto) {
        BillingCycle cycle = new BillingCycle();
        cycle.setAccountId(dto.accountId);
        cycle.setCycleStart(dto.cycleStart);
        cycle.setCycleEnd(dto.cycleEnd);
        cycle.setGeneratedDate(LocalDateTime.now());
        cycle.setStatus("GENERATED");

        // Note: BillingCycle isn't persisted here (no repository existed). If desired, add a BillingCycleRepository.

        // create a single invoice for example
        Invoice inv = new Invoice();
        inv.setAccountId(dto.accountId);
        inv.setCycleId(null);
        inv.setPlanCharges(new BigDecimal("100.00"));
        inv.setExcessCharges(BigDecimal.ZERO);
        inv.setAddOnCharges(BigDecimal.ZERO);
        inv.setTaxes(new BigDecimal("10.00"));
        inv.setTotalAmount(inv.getPlanCharges().add(inv.getTaxes()));
        inv.setDueDate(LocalDate.now().plusDays(30));
        inv.setStatus("UNPAID");

        invoiceRepository.save(inv);

        return cycle;
    }
}
