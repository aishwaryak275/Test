package com.teleConnect.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    private Long accountId;
    private Long cycleId;

    private BigDecimal planCharges = BigDecimal.ZERO;
    private BigDecimal excessCharges = BigDecimal.ZERO;
    private BigDecimal addOnCharges = BigDecimal.ZERO;
    private BigDecimal taxes = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private LocalDate dueDate;
    private String status;

    private BigDecimal lateFee = BigDecimal.ZERO;
    private boolean lateFeeWaived = false;

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }
    public java.math.BigDecimal getPlanCharges() { return planCharges; }
    public void setPlanCharges(java.math.BigDecimal planCharges) { this.planCharges = planCharges; }
    public java.math.BigDecimal getExcessCharges() { return excessCharges; }
    public void setExcessCharges(java.math.BigDecimal excessCharges) { this.excessCharges = excessCharges; }
    public java.math.BigDecimal getAddOnCharges() { return addOnCharges; }
    public void setAddOnCharges(java.math.BigDecimal addOnCharges) { this.addOnCharges = addOnCharges; }
    public java.math.BigDecimal getTaxes() { return taxes; }
    public void setTaxes(java.math.BigDecimal taxes) { this.taxes = taxes; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.math.BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(java.math.BigDecimal lateFee) { this.lateFee = lateFee; }
    public boolean isLateFeeWaived() { return lateFeeWaived; }
    public void setLateFeeWaived(boolean lateFeeWaived) { this.lateFeeWaived = lateFeeWaived; }
}
