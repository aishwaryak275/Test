package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.*;
import com.teleconnect.iam.dto.response.*;
import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired private UserRepository userRepo;
    @Autowired private RoleRepository roleRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private AuditLogService auditLogService;

    @Value("${app.default.staff.password}")
    private String defaultPassword;

    private List<String> getPermissions(Role role) {
        return role.getPermissions().stream()
            .map(Permission::getPermissionName)
            .collect(Collectors.toList());
    }

    // -- REGISTER ---------------------------------------------
    public RegisterResponseDTO register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already in use");

        Role role = roleRepo.findByRoleName("S")
            .orElseThrow(() -> new RuntimeException(
                "Role S (Subscriber) not found - ensure DataLoader has run"));

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

    // -- LOGIN ------------------------------------------------
    public LoginResponseDTO login(LoginRequest req, String ip) {
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        if (user.getStatus() != User.Status.A)
            throw new RuntimeException("Account is not active");

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

    // -- toDTO helper -----------------------------------------
    private UserResponseDTO toDTO(User u) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(u.getUserId());
        dto.setName(u.getName());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setRoleName(u.getRole().getRoleName());
        dto.setRegionId(u.getRegionId());
        dto.setStatus(u.getStatus().name());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }

    // -- CHANGE PASSWORD --------------------------------------
    public MessageDTO changePassword(String email, ChangePasswordRequest req) {
        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new RuntimeException("Current password is incorrect");

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setMustChangePassword(false);
        userRepo.save(user);

        auditLogService.log(user.getUserId(), "PASSWORD_CHANGED", "IAM", "N/A");
        return new MessageDTO("Password changed successfully");
    }

    // -- GET OWN PROFILE --------------------------------------
    public UserResponseDTO getOwnProfile(String email) {
        return toDTO(userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found")));
    }

    // -- GET USER BY ID ---------------------------------------
    public UserResponseDTO getUserById(Long id) {
        return toDTO(userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found")));
    }

    // -- UPDATE USER ------------------------------------------
    public MessageDTO updateUser(Long id, UpdateUserRequest req, boolean isAdmin) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getName() != null) user.setName(req.getName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());

        if (isAdmin) {
            if (req.getRegionId() != null) user.setRegionId(req.getRegionId());
            if (req.getRoleName() != null) {
                Role role = roleRepo.findByRoleName(req.getRoleName().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
                user.setRole(role);
            }
        }

        userRepo.save(user);
        return new MessageDTO("User updated successfully");
    }

    // -- GET ALL USERS ----------------------------------------
    public List<UserResponseDTO> getAllUsers() {
        return userRepo.findAll().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // -- UPDATE STATUS ----------------------------------------
    public MessageDTO updateStatus(Long id, UpdateStatusRequest req) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            user.setStatus(User.Status.valueOf(req.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + req.getStatus());
        }

        userRepo.save(user);
        auditLogService.log(id, "USER_STATUS_CHANGED", "IAM", "ADMIN");
        return new MessageDTO("User status updated to " + req.getStatus().toUpperCase());
    }

    // -- RESET PASSWORD ---------------------------------------
    public MessageDTO resetPassword(Long id) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setMustChangePassword(true);
        userRepo.save(user);

        auditLogService.log(id, "PASSWORD_RESET", "IAM", "ADMIN");
        return new MessageDTO("Password reset to default successfully");
    }

    // -- SEARCH -----------------------------------------------
    public List<UserResponseDTO> searchUsers(String name, String email,
                                             String phone, String status, String roleName) {
        return userRepo.findAll().stream()
            .filter(u -> name == null || u.getName().toLowerCase().contains(name.toLowerCase()))
            .filter(u -> email == null || u.getEmail().toLowerCase().contains(email.toLowerCase()))
            .filter(u -> phone == null || (u.getPhone() != null && u.getPhone().equals(phone)))
            .filter(u -> status == null || u.getStatus().getCode().equalsIgnoreCase(status)
                                        || u.getStatus().getLabel().equalsIgnoreCase(status))
            .filter(u -> roleName == null || u.getRole().getRoleName().equalsIgnoreCase(roleName))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    // -- CREATE STAFF -----------------------------------------
    public RegisterResponseDTO createStaff(CreateStaffRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already in use");

        Role role = roleRepo.findByRoleName(req.getRoleName().toUpperCase())
            .orElseThrow(() -> new RuntimeException(
                "Role not found: " + req.getRoleName()));

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setRole(role);
        user.setRegionId(req.getRegionId());
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setMustChangePassword(true);

        User saved = userRepo.save(user);
        auditLogService.log(saved.getUserId(), "STAFF_ACCOUNT_CREATED", "IAM", "ADMIN");
        return new RegisterResponseDTO("Staff account created successfully");
    }
}
