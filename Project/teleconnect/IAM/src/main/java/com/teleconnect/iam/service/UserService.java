package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.*;
import com.teleconnect.iam.dto.response.*;
import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.exception.DuplicateResourceException;
import com.teleconnect.iam.exception.InvalidRequestException;
import com.teleconnect.iam.exception.ResourceNotFoundException;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepo, RoleRepository roleRepo,
                       PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Value("${app.default.staff.password}")
    private String defaultPassword;

    private List<String> getPermissions(Role role) {
        return role.getPermissions().stream()
            .map(Permission::getPermissionName)
            .collect(Collectors.toList());
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


    // -- GET OWN PROFILE --------------------------------------
    public UserResponseDTO getOwnProfile(String email) {
        return toDTO(userRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    // -- GET USER BY ID ---------------------------------------
    public UserResponseDTO getUserById(Long id) {
        return toDTO(userRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    // -- UPDATE USER ------------------------------------------
    public MessageDTO updateUser(Long id, UpdateUserRequest req, boolean isAdmin) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.getName() != null) user.setName(req.getName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());

        if (isAdmin) {
            if (req.getRegionId() != null) user.setRegionId(req.getRegionId());
            if (req.getRoleName() != null) {
                Role role = roleRepo.findByRoleName(req.getRoleName().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
                user.setRole(role);
            }
        }

        userRepo.save(user);
        auditLogService.log(user.getUserId(), "USER_UPDATED", "IAM", isAdmin ? "ADMIN" : "SELF");
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
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            user.setStatus(User.Status.valueOf(req.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid status: " + req.getStatus());
        }

        userRepo.save(user);
        auditLogService.log(id, "USER_STATUS_CHANGED", "IAM", "ADMIN");
        return new MessageDTO("User status updated to " + req.getStatus().toUpperCase());
    }

    // -- RESET PASSWORD ---------------------------------------
    public MessageDTO resetPassword(Long id) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
            throw new DuplicateResourceException("Email already in use");

        Role role = roleRepo.findByRoleName(req.getRoleName().toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException(
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

    // -- GET ALL ROLES ----------------------------------------
    public List<Role> getAllRoles() {
        return roleRepo.findAll();
    }

    // -- GET ROLE PERMISSIONS ---------------------------------
    public Role getRolePermissions(Integer roleId) {
        return roleRepo.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }
}
