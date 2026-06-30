package com.teleconnect.analytics_service.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_summaries", indexes = {
    @Index(name = "idx_usage_line_cycle", columnList = "line_id, billing_cycle_id")
})
public class UsageSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;

    @Column(nullable = false)
    private Long lineId;

    @Column(nullable = false)
    private Long billingCycleId;

    @Column(nullable = false)
    private Long dataUsedMb;

    @Column(nullable = false)
    private Long voiceUsedMin;

    @Column(nullable = false)
    private Long smsUsed;

    @Column
    private Long dataRemainingMb;

    @Column
    private Long voiceRemainingMin;

    @Column
    private LocalDateTime lastUpdated;

    public UsageSummary() {}

    public Long getSummaryId() { return summaryId; }
    public void setSummaryId(Long summaryId) { this.summaryId = summaryId; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public Long getBillingCycleId() { return billingCycleId; }
    public void setBillingCycleId(Long billingCycleId) { this.billingCycleId = billingCycleId; }

    public Long getDataUsedMb() { return dataUsedMb; }
    public void setDataUsedMb(Long dataUsedMb) { this.dataUsedMb = dataUsedMb; }

    public Long getVoiceUsedMin() { return voiceUsedMin; }
    public void setVoiceUsedMin(Long voiceUsedMin) { this.voiceUsedMin = voiceUsedMin; }

    public Long getSmsUsed() { return smsUsed; }
    public void setSmsUsed(Long smsUsed) { this.smsUsed = smsUsed; }

    public Long getDataRemainingMb() { return dataRemainingMb; }
    public void setDataRemainingMb(Long dataRemainingMb) { this.dataRemainingMb = dataRemainingMb; }

    public Long getVoiceRemainingMin() { return voiceRemainingMin; }
    public void setVoiceRemainingMin(Long voiceRemainingMin) { this.voiceRemainingMin = voiceRemainingMin; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
