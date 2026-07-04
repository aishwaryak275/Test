package com.teleconnect.analytics_service.entity;

import com.teleconnect.analytics_service.enums.BillingCycleStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "billing_cycles")
public class BillingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cycleId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private LocalDate cycleStart;

    @Column(nullable = false)
    private LocalDate cycleEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycleStatus status;

    public BillingCycle() {}

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getCycleStart() { return cycleStart; }
    public void setCycleStart(LocalDate cycleStart) { this.cycleStart = cycleStart; }

    public LocalDate getCycleEnd() { return cycleEnd; }
    public void setCycleEnd(LocalDate cycleEnd) { this.cycleEnd = cycleEnd; }

    public BillingCycleStatus getStatus() { return status; }
    public void setStatus(BillingCycleStatus status) { this.status = status; }
}
