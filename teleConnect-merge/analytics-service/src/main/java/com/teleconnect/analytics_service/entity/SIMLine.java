package com.teleconnect.analytics_service.entity;

import com.teleconnect.analytics_service.enums.SIMStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "sim_lines", indexes = {
    @Index(name = "idx_sim_account_status", columnList = "account_id, status, activation_date")
})
public class SIMLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lineId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 20)
    private String msisdn;

    @Column(length = 20)
    private String iccid;

    @Column(nullable = false)
    private LocalDate activationDate;

    @Column(length = 30)
    private String serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SIMStatus status;

    public SIMLine() {}

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }

    public String getIccid() { return iccid; }
    public void setIccid(String iccid) { this.iccid = iccid; }

    public LocalDate getActivationDate() { return activationDate; }
    public void setActivationDate(LocalDate activationDate) { this.activationDate = activationDate; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public SIMStatus getStatus() { return status; }
    public void setStatus(SIMStatus status) { this.status = status; }
}
