package com.teleconnect.analytics_service.entity;

import com.teleconnect.analytics_service.enums.FaultPriority;
import com.teleconnect.analytics_service.enums.FaultStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fault_tickets", indexes = {
    @Index(name = "idx_fault_raised_priority_status", columnList = "raised_date, priority, status")
})
public class FaultTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    @Column(nullable = false)
    private Long accountId;

    @Column
    private Long lineId;

    @Column(length = 50)
    private String faultType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaultPriority priority;

    @Column(nullable = false)
    private LocalDateTime raisedDate;

    @Column
    private LocalDateTime resolvedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaultStatus status;

    public FaultTicket() {}

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public String getFaultType() { return faultType; }
    public void setFaultType(String faultType) { this.faultType = faultType; }

    public FaultPriority getPriority() { return priority; }
    public void setPriority(FaultPriority priority) { this.priority = priority; }

    public LocalDateTime getRaisedDate() { return raisedDate; }
    public void setRaisedDate(LocalDateTime raisedDate) { this.raisedDate = raisedDate; }

    public LocalDateTime getResolvedDate() { return resolvedDate; }
    public void setResolvedDate(LocalDateTime resolvedDate) { this.resolvedDate = resolvedDate; }

    public FaultStatus getStatus() { return status; }
    public void setStatus(FaultStatus status) { this.status = status; }
}
