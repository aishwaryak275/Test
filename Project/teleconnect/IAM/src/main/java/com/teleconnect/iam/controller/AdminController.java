package com.teleconnect.iam.controller;

import com.teleconnect.iam.dto.request.AuditLogFilterDTO;
import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.dto.request.UpdateStatusRequest;
import com.teleconnect.iam.dto.response.AuditLogResponseDTO;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.service.AuditLogService;
import com.teleconnect.iam.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teleConnect/iam/api")
public class AdminController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AdminController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    // POST /admin/users/createStaff - admin
    @PostMapping("/admin/users/createStaff")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<RegisterResponseDTO> createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(201).body(userService.createStaff(req));
    }

    // PUT /users/{id}/status - admin
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<MessageDTO> updateStatus(@PathVariable Long id,
                                                   @RequestBody UpdateStatusRequest req) {
        return ResponseEntity.ok(userService.updateStatus(id, req));
    }

    // PUT /admin/users/{id}/resetPassword - admin
    @PutMapping("/admin/users/{id}/resetPassword")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<MessageDTO> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(userService.resetPassword(id));
    }

    // GET /roles - admin
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(userService.getAllRoles());
    }

    // GET /roles/{roleId}/permissions - admin
    @GetMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<Role> getRolePermissions(@PathVariable Integer roleId) {
        return ResponseEntity.ok(userService.getRolePermissions(roleId));
        
    }

    // GET /auditLogs - admin, compilance officer
    @GetMapping("/auditLogs")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    public ResponseEntity<List<AuditLogResponseDTO>> getAllLogs(@ModelAttribute AuditLogFilterDTO filter) {
        return ResponseEntity.ok(auditLogService.getAllLogs(filter));
    }

    // GET /auditLogs/user/{userId} - admin, compilance officer
    @GetMapping("/auditLogs/user/{userId}")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    public ResponseEntity<List<AuditLogResponseDTO>> getLogsByUser(@PathVariable Long userId,
                                                                   @ModelAttribute AuditLogFilterDTO filter) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(userId, filter));
    }
}
