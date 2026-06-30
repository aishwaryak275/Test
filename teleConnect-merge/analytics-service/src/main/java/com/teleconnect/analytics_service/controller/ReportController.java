package com.teleconnect.analytics_service.controller;

import com.teleconnect.analytics_service.dto.request.ReportGenerationRequest;
import com.teleconnect.analytics_service.dto.response.ApiResponse;
import com.teleconnect.analytics_service.dto.response.TelecomReportResponse;
import com.teleconnect.analytics_service.enums.ReportScope;
import com.teleconnect.analytics_service.service.ReportService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    private AuditClient auditClient;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * POST /api/reports/generate
     * Trigger on-demand report generation.
     * Roles: Admin, Billing, NetworkOps
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TelecomReportResponse>> generateReport(
            @Valid @RequestBody ReportGenerationRequest request,
            HttpServletRequest httpReq) {
        TelecomReportResponse result = reportService.generateReport(request);
        auditClient.record(AuditAction.GENERATE_REPORT, AuditModule.ANALYTICS, httpReq);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Report generated successfully", result));
    }

    /**
     * GET /api/reports/{reportId}
     * Fetch a previously stored TelecomReport by ID.
     * Roles: All privileged roles
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<TelecomReportResponse>> getReport(@PathVariable Long reportId) {
        TelecomReportResponse result = reportService.getReportById(reportId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/reports?scope=&from=&to=&page=&size=
     * Paginated list of historical report snapshots.
     * Roles: Admin, Compliance
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TelecomReportResponse>>> listReports(
            @RequestParam(required = false) ReportScope scope,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TelecomReportResponse> result = reportService.listReports(scope, from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
