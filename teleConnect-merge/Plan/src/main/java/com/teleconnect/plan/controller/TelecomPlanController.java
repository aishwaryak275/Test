package com.teleconnect.plan.controller;

import com.teleconnect.plan.dto.request.TelecomPlanRequest;
import com.teleconnect.plan.dto.response.TelecomPlanResponse;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.service.TelecomPlanService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/plan")
public class TelecomPlanController {

    @Autowired
    private TelecomPlanService service;

    @Autowired
    private AuditClient auditClient;

    @PostMapping("/createPlans")
    public ResponseEntity<?> createPlan(
            @RequestBody TelecomPlanRequest req,
            HttpServletRequest httpReq) {
        if (req.getName() == null || req.getName().isBlank())
            return ResponseEntity.status(400)
                .body(new MessageResponse("name is required"));
        if (req.getType() == null)
            return ResponseEntity.status(400)
                .body(new MessageResponse("type must be Postpaid or Prepaid"));
        if (req.getPlanPrice() == null)
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "planPrice must be a positive number"));
        if (req.getValidityDays() == null)
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "validityDays must be a positive integer"));
        try {
            service.createPlan(req);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                .body(new MessageResponse("type must be Postpaid or Prepaid"));
        }
        auditClient.record(AuditAction.CREATE_PLAN, AuditModule.PLAN, httpReq);
        return ResponseEntity.status(201)
            .body(new MessageResponse("Plan created successfully"));
    }

    @GetMapping("/getAllPlans")
    public ResponseEntity<?> getAllPlans() {
        List<TelecomPlanResponse> plans = service.getAllPlans();
        if (plans.isEmpty())
            return ResponseEntity.status(404)
                .body(new MessageResponse("No plans found"));
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/getPlans/{planId}")
    public ResponseEntity<?> getPlanById(
            @PathVariable Integer planId) {
        TelecomPlanResponse plan = service.getPlanById(planId);
        if (plan == null)
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Plan with planId " + planId + " not found"));
        return ResponseEntity.ok(plan);
    }

    @PutMapping("/updatePlans/{planId}")
    public ResponseEntity<?> updatePlan(
            @PathVariable Integer planId,
            @RequestBody TelecomPlanRequest req,
            HttpServletRequest httpReq) {
        boolean updated = service.updatePlan(planId, req);
        if (!updated)
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Plan with planId " + planId + " not found"));
        auditClient.record(AuditAction.UPDATE_PLAN, AuditModule.PLAN, httpReq);
        return ResponseEntity.ok(
            new MessageResponse("Plan updated successfully"));
    }
}