package com.teleconnect.plan.controller;

import com.teleconnect.plan.dto.request.AddOnRequest;
import com.teleconnect.plan.dto.response.AddOnResponse;
import com.teleconnect.plan.dto.response.MessageResponse;
import com.teleconnect.plan.service.AddOnService;
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
public class AddOnController {

    @Autowired
    private AddOnService service;

    @Autowired
    private AuditClient auditClient;

    @PostMapping("/createAddOns")
    public ResponseEntity<?> createAddOn(
            @RequestBody AddOnRequest req,
            HttpServletRequest httpReq) {
        if (req.getName() == null || req.getName().isBlank())
            return ResponseEntity.status(400)
                .body(new MessageResponse("name is required"));
        if (req.getType() == null)
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "type must be DataTopup, ISDPack, RoamingPack, or SMSPack"));
        if (req.getQuota() == null)
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "quota must be a positive number"));
        if (req.getPrice() == null)
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "price must be a positive number"));
        try {
            service.createAddOn(req);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "type must be DataTopup, ISDPack, RoamingPack, or SMSPack"));
        }
        auditClient.record(AuditAction.CREATE_ADDON, AuditModule.PLAN, httpReq);
        return ResponseEntity.status(201)
            .body(new MessageResponse("Add-on created successfully"));
    }

    @GetMapping("/getAllAddOns")
    public ResponseEntity<?> getAllAddOns() {
        List<AddOnResponse> addOns = service.getAllAddOns();
        if (addOns.isEmpty())
            return ResponseEntity.status(404)
                .body(new MessageResponse("No add-ons found"));
        return ResponseEntity.ok(addOns);
    }

    @GetMapping("/getAddOns/{addOnId}")
    public ResponseEntity<?> getAddOnById(
            @PathVariable Integer addOnId) {
        AddOnResponse addOn = service.getAddOnById(addOnId);
        if (addOn == null)
            return ResponseEntity.status(404)
                .body(new MessageResponse(
                    "Add-on with addOnId " + addOnId + " not found"));
        return ResponseEntity.ok(addOn);
    }
}