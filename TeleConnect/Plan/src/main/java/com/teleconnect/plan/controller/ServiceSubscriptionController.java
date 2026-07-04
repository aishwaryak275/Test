package com.teleconnect.plan.controller;

import com.teleconnect.plan.dto.request.ServiceSubscriptionRequest;
import com.teleconnect.plan.dto.response.ServiceSubscriptionResponse;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.service.ServiceSubscriptionService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/plan")
public class ServiceSubscriptionController {

    private final ServiceSubscriptionService service;
    private final AuditClient auditClient;

    public ServiceSubscriptionController(ServiceSubscriptionService service, AuditClient auditClient) {
        this.service = service;
        this.auditClient = auditClient;
    }

    @PostMapping("/createSubscriptions")
    @PreAuthorize("hasAuthority('CREATE_SUB')")
    public ResponseEntity<?> createSubscription(
            @RequestBody ServiceSubscriptionRequest req,
            HttpServletRequest httpReq) {
        String error = service.validate(req);
        if (error != null)
            return ResponseEntity.status(
                error.contains("not found") ? 404 : 400)
                .body(new MessageResponse(error));
        service.createSubscription(req);
        auditClient.record(AuditAction.CREATE_SUBSCRIPTION, AuditModule.PLAN, httpReq);
        return ResponseEntity.status(201)
            .body(new MessageResponse("Subscription created successfully"));
    }

    @GetMapping("/getAllSubscriptions")
    @PreAuthorize("hasAuthority('GET_SUB') and !hasAuthority('VIEW_PLAN')")
    public ResponseEntity<?> getAllSubscriptions() {
        List<ServiceSubscriptionResponse> list =
            service.getAllSubscriptions();
        if (list.isEmpty())
            return ResponseEntity.status(404)
                .body(new MessageResponse("No subscriptions found"));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/getSubscriptions/{subscriptionId}")
    @PreAuthorize("hasAuthority('GET_SUB')")
    public ResponseEntity<?> getSubscriptionById(
            @PathVariable Integer subscriptionId) {
        ServiceSubscriptionResponse sub =
            service.getById(subscriptionId);
        if (sub == null)
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Subscription with subscriptionId "
                    + subscriptionId + " not found"));
        return ResponseEntity.ok(sub);
    }

    @PutMapping("/updateSubscriptions/{subscriptionId}")
    @PreAuthorize("hasAuthority('CREATE_SUB')")
    public ResponseEntity<?> updateSubscription(
            @PathVariable Integer subscriptionId,
            @RequestBody ServiceSubscriptionRequest req,
            HttpServletRequest httpReq) {
        boolean updated =
            service.updateSubscription(subscriptionId, req);
        if (!updated)
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Subscription with subscriptionId "
                    + subscriptionId + " not found"));
        auditClient.record(AuditAction.UPDATE_SUBSCRIPTION, AuditModule.PLAN, httpReq);
        return ResponseEntity.ok(
            new MessageResponse("Subscription updated successfully"));
    }
}