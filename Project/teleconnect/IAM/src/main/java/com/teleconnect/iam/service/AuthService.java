package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.*;
import com.teleconnect.iam.dto.response.*;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.exception.*;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;


    public AuthService(UserRepository userRepo,
                       RoleRepository roleRepo,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuditLogService auditLogService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogService = auditLogService;
    }

    private List<String> getPermissions(Role role) {
        return role.getPermissions().stream()
            .map(Permission::getPermissionName)
            .collect(Collectors.toList());
    }

    // REGISTER
    public RegisterResponseDTO register(RegisterRequest req) {

        if (userRepo.existsByEmail(req.getEmail()))
            throw new DuplicateResourceException("Email already in use");

        Role role = roleRepo.findByRoleName("S")
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole(role);
        user.setRegionId(req.getRegionId());
        user.setMustChangePassword(false);

        User saved = userRepo.save(user);

        auditLogService.log(saved.getUserId(), "USER_REGISTERED", "IAM", "N/A");

        return new RegisterResponseDTO("Registration successful");
    }

    // LOGIN
    public LoginResponseDTO login(LoginRequest req, String ip) {

        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new InvalidCredentialsException("Invalid credentials");

        if (user.getStatus() != User.Status.A)
            throw new AccountNotActiveException("Account not active");

        List<String> permissions = getPermissions(user.getRole());
        String token = jwtUtil.generateToken(user.getEmail(), permissions);

        auditLogService.log(user.getUserId(), "USER_LOGIN", "IAM", ip);

        return new LoginResponseDTO(
            token,
            user.getRole().getRoleName(),
            user.getName(),
            user.getMustChangePassword(),
            permissions
        );
    }

    // LOGOUT
    public MessageDTO logout(String email) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        auditLogService.log(user.getUserId(), "USER_LOGOUT", "IAM", "N/A");

        return new MessageDTO("Logged out successfully");
    }

    // CHANGE PASSWORD
    public MessageDTO changePassword(String email, ChangePasswordRequest req) {

        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new InvalidCredentialsException("Invalid current password");

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);

        userRepo.save(user);

        auditLogService.log(user.getUserId(), "PASSWORD_CHANGED", "IAM", "N/A");

        return new MessageDTO("Password changed successfully");
    }
}
