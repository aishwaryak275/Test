package com.teleconnect.usage.controller;

import com.teleconnect.usage.dto.request.UsageRecordRequest;
import com.teleconnect.usage.dto.response.*;
import com.teleconnect.usage.service.UsageService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/teleConnect/usage")
public class UsageController {
        private final UsageService usageService;
        private final AuditClient auditClient;

        public UsageController(UsageService usageService, AuditClient auditClient) {
                this.usageService = usageService;
                this.auditClient = auditClient;
        }

        // USAGE RECORDING
        @PostMapping("/createRecord")
        @PreAuthorize("hasAuthority('CREATE_USER')")
        public ResponseEntity<Map<String, String>> createRecord(
                        @Valid @RequestBody UsageRecordRequest req, HttpServletRequest httpReq) {
                var result = ResponseEntity.status(HttpStatus.CREATED)
                                .body(usageService.createUsageRecord(req));
                auditClient.record(AuditAction.CREATE_USAGE_RECORD, AuditModule.USAGE, httpReq);
                return result;
        }

        @GetMapping("/fetchRecords/{lineId}")
        @PreAuthorize("hasAuthority('USAGE_RECORDS')")
        public ResponseEntity<Map<String, Object>> fetchByLine(@PathVariable Long lineId) {
                return ResponseEntity.ok(Map.of("lineId", lineId,
                                "records", usageService.fetchRecordsByLine(lineId)));
        }

        @GetMapping("/fetchRecords/{lineId}/{billingCycleId}")
        @PreAuthorize("hasAuthority('USAGE_RECORDS')")
        public ResponseEntity<Map<String, Object>> fetchByCycle(
                        @PathVariable Long lineId, @PathVariable Long billingCycleId) {
                return ResponseEntity.ok(Map.of(
                                "lineId", lineId, "billingCycleId", billingCycleId,
                                "records", usageService.fetchRecordsByCycle(lineId, billingCycleId)));
        }

        // USAGE SUMMARY
        @GetMapping("/fetchSummary/{lineId}/{billingCycleId}")
        @PreAuthorize("hasAuthority('USAGE_RECORDS')")
        public ResponseEntity<UsageSummaryResponse> fetchSummary(
                        @PathVariable Long lineId, @PathVariable Long billingCycleId) {
                return ResponseEntity.ok(usageService.fetchSummary(lineId, billingCycleId));
        }

        @PutMapping("/updateSummary/{lineId}/{billingCycleId}")
        @PreAuthorize("hasAuthority('CREATE_USER')")
        public ResponseEntity<Map<String, Object>> updateSummary(
                        @PathVariable Long lineId, @PathVariable Long billingCycleId,
                        @RequestBody Map<String, Object> body, HttpServletRequest httpReq) {
                BigDecimal dataUsedMb = body.get("dataUsedMb") != null
                                ? new BigDecimal(body.get("dataUsedMb").toString())
                                : null;
                BigDecimal voiceUsedMin = body.get("voiceUsedMin") != null
                                ? new BigDecimal(body.get("voiceUsedMin").toString())
                                : null;
                Integer smsUsed = body.get("smsUsed") != null ? Integer.parseInt(body.get("smsUsed").toString()) : null;
                var result = ResponseEntity.ok(usageService.updateSummary(
                                lineId, billingCycleId, dataUsedMb, voiceUsedMin, smsUsed));
                auditClient.record(AuditAction.UPDATE_USAGE_SUMMARY, AuditModule.USAGE, httpReq);
                return result;
        }

        // PLAN LIMIT TRACKING
        // Pass plan limits as query params:
        // ?dataLimitMb=5120&voiceLimitMin=300&smsLimit=100
        @GetMapping("/limitStatus/{lineId}/{billingCycleId}")
        @PreAuthorize("hasAuthority('USAGE_RECORDS')")
        public ResponseEntity<LimitStatusResponse> getLimitStatus(
                        @PathVariable Long lineId, @PathVariable Long billingCycleId,
                        @RequestParam double dataLimitMb, @RequestParam double voiceLimitMin,
                        @RequestParam int smsLimit) {
                return ResponseEntity.ok(usageService.getLimitStatus(
                                lineId, billingCycleId, dataLimitMb, voiceLimitMin, smsLimit));
        }

        @GetMapping("/remaining/{lineId}/{billingCycleId}")
        @PreAuthorize("hasAuthority('USAGE_RECORDS')")
        public ResponseEntity<Map<String, Object>> getRemaining(
                        @PathVariable Long lineId, @PathVariable Long billingCycleId) {
                return ResponseEntity.ok(usageService.getRemainingQuota(lineId, billingCycleId));
        }

        // ALERTS
        @GetMapping("/alerts/{lineId}/{billingCycleId}")
        @PreAuthorize("hasAuthority('USAGE_RECORDS')")
        public ResponseEntity<AlertResponse> getAlerts(
                        @PathVariable Long lineId, @PathVariable Long billingCycleId,
                        @RequestParam double dataLimitMb, @RequestParam double voiceLimitMin,
                        @RequestParam int smsLimit) {
                return ResponseEntity.ok(usageService.getThresholdAlerts(
                                lineId, billingCycleId, dataLimitMb, voiceLimitMin, smsLimit));
        }

        // ANALYTICS
        @GetMapping("/analytics/{lineId}")
        @PreAuthorize("hasAuthority('USAGE_ANALYTICS')")
        public ResponseEntity<AnalyticsTrendResponse> getAnalyticsTrend(
                        @PathVariable Long lineId) {
                return ResponseEntity.ok(usageService.getUsageTrend(lineId));
        }

        // GET /teleConnect/usage/analytics/{lineId}/top-usage
        @GetMapping("/analytics/{lineId}/top-usage")
        @PreAuthorize("hasAuthority('USAGE_ANALYTICS')")
        public ResponseEntity<Map<String, Object>> getTopUsage(
                        @PathVariable Long lineId) {
                return ResponseEntity.ok(usageService.getTopUsage(lineId));
        }
}
