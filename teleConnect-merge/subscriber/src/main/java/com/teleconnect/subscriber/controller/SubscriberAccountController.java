package com.teleconnect.subscriber.controller;

import com.teleconnect.subscriber.dto.request.*;
import com.teleconnect.subscriber.dto.response.*;
import com.teleconnect.subscriber.service.SubscriberAccountService;
import com.teleconnect.common.audit.AuditAction;
import com.teleconnect.common.audit.AuditModule;
import com.teleconnect.common.audit.AuditClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/teleConnect/api/subscribers")
public class SubscriberAccountController {

    private final SubscriberAccountService accountService;
    private final AuditClient auditClient;

    public SubscriberAccountController(SubscriberAccountService accountService, AuditClient auditClient) {
        this.accountService = accountService;
        this.auditClient = auditClient;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CREATE_USER','VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> createAccount(
            @Valid @RequestBody CreateAccountRequest req,
            HttpServletRequest httpReq) {
        var result = accountService.createAccount(req);
        auditClient.record(AuditAction.CREATE_SUBSCRIBER_ACCOUNT, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.status(201)
            .body(result);
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER')")
    public ResponseEntity<AccountResponseDTO> getAccount(
            @PathVariable Integer accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER', 'VIEW_OWN_PLAN')")
    public ResponseEntity<AccountListResponseDTO> getAllAccounts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long subscriberId) {
        return ResponseEntity.ok(
            accountService.getAllAccounts(status, subscriberId));
    }

    @GetMapping("/kyc/expired")
    @PreAuthorize("hasAnyAuthority('KYC_EXPIRE')")
    public ResponseEntity<List<AccountResponseDTO>> getExpiredKyc() {
        return ResponseEntity.ok(accountService.getExpiredKycAccounts());
    }

    @PutMapping("/{accountId}/kyc")
    @PreAuthorize("hasAnyAuthority('VIEW_KYC')")
    public ResponseEntity<MessageDTO> updateKyc(
            @PathVariable Integer accountId,
            @Valid @RequestBody UpdateKycRequest req,
            HttpServletRequest httpReq) {
        var result = accountService.updateKyc(accountId, req);
        auditClient.record(AuditAction.UPDATE_KYC_STATUS, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{accountId}/status")
    @PreAuthorize("hasAnyAuthority('VIEW_SUBSCRIBER')")
    public ResponseEntity<MessageDTO> updateStatus(
            @PathVariable Integer accountId,
            @Valid @RequestBody UpdateAccountStatusRequest req,
            HttpServletRequest httpReq) {
        var result = accountService.updateStatus(accountId, req);
        auditClient.record(AuditAction.UPDATE_ACCOUNT_STATUS, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<MessageDTO> deleteAccount(
            @PathVariable Integer accountId,
            HttpServletRequest httpReq) {
        var result = accountService.deleteAccount(accountId);
        auditClient.record(AuditAction.DELETE_SUBSCRIBER_ACCOUNT, AuditModule.SUBSCRIBER, httpReq);
        return ResponseEntity.ok(result);
    }
}