package com.teleConnect.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disputeID;

    private Long invoiceID;
    private Long subscriberID;
    private BigDecimal disputedAmount;
    private LocalDateTime raisedDate;
    private String status;

    public Long getDisputeID() { return disputeID; }
    public void setDisputeID(Long disputeID) { this.disputeID = disputeID; }
    public Long getInvoiceID() { return invoiceID; }
    public void setInvoiceID(Long invoiceID) { this.invoiceID = invoiceID; }
    public Long getSubscriberID() { return subscriberID; }
    public void setSubscriberID(Long subscriberID) { this.subscriberID = subscriberID; }
    public BigDecimal getDisputedAmount() { return disputedAmount; }
    public void setDisputedAmount(BigDecimal disputedAmount) { this.disputedAmount = disputedAmount; }
    public LocalDateTime getRaisedDate() { return raisedDate; }
    public void setRaisedDate(LocalDateTime raisedDate) { this.raisedDate = raisedDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
