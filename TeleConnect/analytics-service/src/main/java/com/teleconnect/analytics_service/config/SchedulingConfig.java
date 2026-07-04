package com.teleconnect.analytics_service.config;

import com.teleconnect.analytics_service.service.SLAComplianceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);

    private final SLAComplianceService slaComplianceService;

    public SchedulingConfig(SLAComplianceService slaComplianceService) {
        this.slaComplianceService = slaComplianceService;
    }

    /** Run every hour to check and escalate tickets breaching SLA targets. */
    @Scheduled(fixedRateString = "PT1H")
    public void scheduledSLAEscalation() {
        log.info("Running scheduled SLA breach escalation check");
        try {
            slaComplianceService.escalateBreachingTickets();
        } catch (Exception e) {
            log.error("SLA escalation job failed: {}", e.getMessage());
        }
    }
}
