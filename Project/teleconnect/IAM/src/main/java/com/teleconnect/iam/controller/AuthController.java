package com.teleconnect.iam.controller;

import com.teleconnect.iam.dto.request.ChangePasswordRequest;
import com.teleconnect.iam.dto.request.LoginRequest;
import com.teleconnect.iam.dto.request.RegisterRequest;
import com.teleconnect.iam.dto.response.LoginResponseDTO;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.exception.AuthenticationRequiredException;
import com.teleconnect.iam.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/teleConnect/iam/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /auth/register - subscriber
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(201).body(authService.register(req));
    }

    // POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequest req,
                                                  HttpServletRequest http) {
        return ResponseEntity.ok(authService.login(req, http.getRemoteAddr()));
    }

    // POST /auth/logout 
    @PostMapping("/logout")
    public ResponseEntity<MessageDTO> logout(Principal principal) {
        if (principal == null) {
            throw new AuthenticationRequiredException("Not authenticated");
        }
        return ResponseEntity.ok(authService.logout(principal.getName()));
        
    }

    // PUT /auth/changePassword 
    @PutMapping("/changePassword")
    public ResponseEntity<MessageDTO> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                                     Principal principal) {
        if (principal == null) {
            throw new AuthenticationRequiredException("Not authenticated");
        }
        return ResponseEntity.ok(authService.changePassword(principal.getName(), req));
    }
}
