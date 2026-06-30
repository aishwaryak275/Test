package com.teleconnect.iam.controller;

import com.teleconnect.iam.dto.request.AuditLogFilterDTO;
import com.teleconnect.iam.dto.request.AuditRecordRequest;
import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.dto.request.UpdateStatusRequest;
import com.teleconnect.iam.dto.response.AuditLogResponseDTO;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.service.AuditLogService;
import com.teleconnect.iam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teleConnect/iam/api")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private RoleRepository roleRepo;
    @Autowired private AuditLogService auditLogService;
    @Autowired private UserRepository userRepo;

    // POST /admin/users/createStaff — Admin only (CREATE_USER permission)
    @PostMapping("/admin/users/createStaff")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<RegisterResponseDTO> createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return ResponseEntity.status(201).body(userService.createStaff(req));
    }

    // PUT /users/{id}/status — Admin only (DELETE_USER)
    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<MessageDTO> updateStatus(@PathVariable Long id,
                                                   @RequestBody UpdateStatusRequest req) {
        return ResponseEntity.ok(userService.updateStatus(id, req));
    }

    // PUT /admin/users/{id}/resetPassword — Admin only (CREATE_USER)
    @PutMapping("/admin/users/{id}/resetPassword")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<MessageDTO> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(userService.resetPassword(id));
    }

    // GET /roles — Admin only (VIEW_ALL_USERS)
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(roleRepo.findAll());
    }

    // GET /roles/{roleId}/permissions — Admin only (VIEW_ALL_USERS)
    @GetMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('VIEW_ALL_USERS')")
    public ResponseEntity<Role> getRolePermissions(@PathVariable Integer roleId) {
        return ResponseEntity.ok(roleRepo.findById(roleId)
            .orElseThrow(() -> new RuntimeException("Role not found")));
    }

    // GET /auditLogs — Compliance + Admin (VIEW_AUDIT_LOGS)
    @GetMapping("/auditLogs")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    public ResponseEntity<List<AuditLogResponseDTO>> getAllLogs(@ModelAttribute AuditLogFilterDTO filter) {
        return ResponseEntity.ok(auditLogService.getAllLogs(filter));
    }

    // GET /auditLogs/user/{userId} — Compliance + Admin (VIEW_AUDIT_LOGS)
    @GetMapping("/auditLogs/user/{userId}")
    @PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
    public ResponseEntity<List<AuditLogResponseDTO>> getLogsByUser(@PathVariable Long userId,
                                                                   @ModelAttribute AuditLogFilterDTO filter) {
        return ResponseEntity.ok(auditLogService.getLogsByUser(userId, filter));
    }

    // POST /auditLogs — central audit sink for ALL modules.
    // Any authenticated caller may record an action; the userId is resolved here
    // from the JWT (email -> userId) so callers never need to know the numeric id.
    @PostMapping("/auditLogs")
    public ResponseEntity<Void> recordAudit(@RequestBody AuditRecordRequest req,
                                            HttpServletRequest httpReq) {
        Long userId = resolveCurrentUserId();
        String ip = (req.getIpAddress() != null && !req.getIpAddress().isBlank())
                ? req.getIpAddress()
                : httpReq.getRemoteAddr();
        auditLogService.log(userId, req.getAction(), req.getModule(), ip);
        return ResponseEntity.status(201).build();
    }

    // Resolve the numeric userId of the currently authenticated principal (email).
    private Long resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return userRepo.findByEmail(auth.getName()).map(User::getUserId).orElse(null);
    }
}
