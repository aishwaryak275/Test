package com.teleconnect.analytics_service.controller;

import com.teleconnect.analytics_service.dto.response.*;
import com.teleconnect.analytics_service.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class AnalyticsController {

    private final ARPUService arpuService;
    private final ChurnService churnService;
    private final NetworkUtilisationService networkUtilisationService;
    private final SLAComplianceService slaComplianceService;
    private final CollectionEfficiencyService collectionEfficiencyService;
    private final SubscriberGrowthService subscriberGrowthService;

    public AnalyticsController(ARPUService arpuService,
                              ChurnService churnService,
                              NetworkUtilisationService networkUtilisationService,
                              SLAComplianceService slaComplianceService,
                              CollectionEfficiencyService collectionEfficiencyService,
                              SubscriberGrowthService subscriberGrowthService) {
        this.arpuService = arpuService;
        this.churnService = churnService;
        this.networkUtilisationService = networkUtilisationService;
        this.slaComplianceService = slaComplianceService;
        this.collectionEfficiencyService = collectionEfficiencyService;
        this.subscriberGrowthService = subscriberGrowthService;
    }

    /**
     * GET /api/reports/arpu?cycleId=&scope=&scopeValue=
     * Roles: Admin, Billing
     */
    @GetMapping("/arpu")
    public ResponseEntity<ApiResponse<ARPUReportResponse>> getARPU(
            @RequestParam Long cycleId,
            @RequestParam(required = false, defaultValue = "PERIOD") String scope,
            @RequestParam(required = false, defaultValue = "ALL") String scopeValue) {
        ARPUReportResponse result = arpuService.computeARPU(cycleId, scope, scopeValue);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/reports/churn?periodStart=&periodEnd=&region=
     * Roles: Admin, Compliance
     */
    @GetMapping("/churn")
    public ResponseEntity<ApiResponse<ChurnReportResponse>> getChurn(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
            @RequestParam(required = false) String region) {
        ChurnReportResponse result = churnService.computeChurn(periodStart, periodEnd, region);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/reports/network-utilisation?cycleId=&region=
     * Roles: Admin, NetworkOps
     */
    @GetMapping("/network-utilisation")
    public ResponseEntity<ApiResponse<NetworkUtilisationResponse>> getNetworkUtilisation(
            @RequestParam Long cycleId,
            @RequestParam(required = false) String region) {
        NetworkUtilisationResponse result = networkUtilisationService.computeUtilisation(cycleId, region);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/reports/sla-compliance?periodStart=&periodEnd=
     * Roles: Admin, NetworkOps, Compliance
     */
    @GetMapping("/sla-compliance")
    public ResponseEntity<ApiResponse<SLAComplianceResponse>> getSLACompliance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        SLAComplianceResponse result = slaComplianceService.computeSLACompliance(periodStart, periodEnd);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/reports/collection-efficiency?cycleId=
     * Roles: Admin, Billing
     */
    @GetMapping("/collection-efficiency")
    public ResponseEntity<ApiResponse<CollectionEfficiencyResponse>> getCollectionEfficiency(
            @RequestParam Long cycleId) {
        CollectionEfficiencyResponse result = collectionEfficiencyService.computeCollectionEfficiency(cycleId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/reports/subscriber-growth?periodStart=&periodEnd=
     * Roles: Admin, Compliance
     */
    @GetMapping("/subscriber-growth")
    public ResponseEntity<ApiResponse<SubscriberGrowthResponse>> getSubscriberGrowth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd) {
        SubscriberGrowthResponse result = subscriberGrowthService.computeGrowth(periodStart, periodEnd);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
