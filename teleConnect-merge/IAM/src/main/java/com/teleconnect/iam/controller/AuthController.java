package com.teleconnect.iam.controller;

import com.teleconnect.iam.dto.request.ChangePasswordRequest;
import com.teleconnect.iam.dto.request.LoginRequest;
import com.teleconnect.iam.dto.request.RegisterRequest;
import com.teleconnect.iam.dto.response.LoginResponseDTO;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.service.AuditLogService;
import com.teleconnect.iam.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/teleConnect/iam/api/auth")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private UserRepository userRepo;

    // POST /auth/register
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(201).body(userService.register(req));
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequest req,
                                                  HttpServletRequest http) {
        return ResponseEntity.ok(userService.login(req, http.getRemoteAddr()));
    }

    // POST /auth/logout — JWT is stateless, so just record the audit entry
    @PostMapping("/logout")
    public ResponseEntity<MessageDTO> logout(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Not authenticated");
        }

        User user = userRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Pass the real userId so audit_logs.user_id is never null
        auditLogService.log(user.getUserId(), "USER_LOGOUT", "IAM", "N/A");

        return ResponseEntity.ok(new MessageDTO("Logged out successfully"));
    }

    // PUT /auth/changePassword — any logged-in user; email comes from the JWT
    @PutMapping("/changePassword")
    public ResponseEntity<MessageDTO> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                                     Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Not authenticated");
        }
        return ResponseEntity.ok(userService.changePassword(principal.getName(), req));
    }
}
