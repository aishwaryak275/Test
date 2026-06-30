package com.teleConnect.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentID;

    private Long invoiceID;
    private BigDecimal amountPaid;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String transactionRef;
    private String status;

    public Long getPaymentID() { return paymentID; }
    public void setPaymentID(Long paymentID) { this.paymentID = paymentID; }
    public Long getInvoiceID() { return invoiceID; }
    public void setInvoiceID(Long invoiceID) { this.invoiceID = invoiceID; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
