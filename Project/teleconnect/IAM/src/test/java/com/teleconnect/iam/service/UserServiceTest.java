package com.teleconnect.iam.service;

import com.teleconnect.iam.dto.request.ChangePasswordRequest;
import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.dto.request.LoginRequest;
import com.teleconnect.iam.dto.request.RegisterRequest;
import com.teleconnect.iam.dto.request.UpdateStatusRequest;
import com.teleconnect.iam.dto.request.UpdateUserRequest;
import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.exception.AccountNotActiveException;
import com.teleconnect.iam.exception.DuplicateResourceException;
import com.teleconnect.iam.exception.InvalidCredentialsException;
import com.teleconnect.iam.exception.InvalidRequestException;
import com.teleconnect.iam.exception.ResourceNotFoundException;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exception-path coverage for {@link UserService}. Every throw / orElseThrow
 * site in the service is exercised here and asserted to throw the correct typed
 * IAM exception with the exact message the GlobalExceptionHandler relays.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private UserService userService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "defaultPassword", "Default@123");

        Role role = new Role();
        role.setRoleName("S");

        activeUser = new User();
        activeUser.setUserId(1L);
        activeUser.setEmail("user@test.com");
        activeUser.setPassword("hashed");
        activeUser.setRole(role);
        activeUser.setStatus(User.Status.A);
    }

    // ---- REGISTER ----------------------------------------------------------
    @Nested
    @DisplayName("register()")
    class Register {
        @Test
        @DisplayName("duplicate email -> 'Email already in use'")
        void duplicateEmail() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("dup@test.com");
            when(userRepo.existsByEmail("dup@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.register(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Email already in use");
        }

        @Test
        @DisplayName("missing Subscriber role -> 'Role S ... not found'")
        void roleNotFound() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@test.com");
            req.setPassword("pw");
            when(userRepo.existsByEmail("new@test.com")).thenReturn(false);
            when(roleRepo.findByRoleName("S")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.register(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role S (Subscriber) not found");
        }
    }

    // ---- LOGIN -------------------------------------------------------------
    @Nested
    @DisplayName("login()")
    class Login {
        @Test
        @DisplayName("unknown email -> 'User not found'")
        void userNotFound() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ghost@test.com");
            when(userRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(req, "127.0.0.1"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("wrong password -> 'Invalid credentials'")
        void invalidCredentials() {
            LoginRequest req = new LoginRequest();
            req.setEmail("user@test.com");
            req.setPassword("wrong");
            when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> userService.login(req, "127.0.0.1"))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("inactive account -> 'Account is not active'")
        void accountNotActive() {
            activeUser.setStatus(User.Status.S);
            LoginRequest req = new LoginRequest();
            req.setEmail("user@test.com");
            req.setPassword("pw");
            when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);

            assertThatThrownBy(() -> userService.login(req, "127.0.0.1"))
                    .isInstanceOf(AccountNotActiveException.class)
                    .hasMessage("Account is not active");
        }
    }

    // ---- CHANGE PASSWORD ---------------------------------------------------
    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {
        @Test
        @DisplayName("unknown user -> 'User not found'")
        void userNotFound() {
            when(userRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.changePassword("ghost@test.com", new ChangePasswordRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("wrong current password -> 'Current password is incorrect'")
        void currentPasswordIncorrect() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("wrong");
            req.setNewPassword("new");
            when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword("user@test.com", req))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Current password is incorrect");
        }
    }

    // ---- LOOKUPS -----------------------------------------------------------
    @Nested
    @DisplayName("lookups")
    class Lookups {
        @Test
        @DisplayName("getOwnProfile() unknown -> 'User not found'")
        void getOwnProfile() {
            when(userRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getOwnProfile("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("getUserById() unknown -> 'User not found'")
        void getUserById() {
            when(userRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    // ---- UPDATE USER -------------------------------------------------------
    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {
        @Test
        @DisplayName("unknown user -> 'User not found'")
        void userNotFound() {
            when(userRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.updateUser(99L, new UpdateUserRequest(), true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("admin sets unknown role -> 'Role not found'")
        void roleNotFound() {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setRoleName("ghost");
            when(userRepo.findById(1L)).thenReturn(Optional.of(activeUser));
            when(roleRepo.findByRoleName("GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(1L, req, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Role not found");
        }
    }

    // ---- UPDATE STATUS -----------------------------------------------------
    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {
        @Test
        @DisplayName("unknown user -> 'User not found'")
        void userNotFound() {
            UpdateStatusRequest req = new UpdateStatusRequest();
            req.setStatus("A");
            when(userRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateStatus(99L, req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("bad status string -> 'Invalid status: ...' (wraps IllegalArgumentException)")
        void invalidStatus() {
            UpdateStatusRequest req = new UpdateStatusRequest();
            req.setStatus("BOGUS");
            when(userRepo.findById(1L)).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> userService.updateStatus(1L, req))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage("Invalid status: BOGUS");
        }
    }

    // ---- RESET PASSWORD ----------------------------------------------------
    @Test
    @DisplayName("resetPassword() unknown user -> 'User not found'")
    void resetPassword_userNotFound() {
        when(userRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    // ---- CREATE STAFF ------------------------------------------------------
    @Nested
    @DisplayName("createStaff()")
    class CreateStaff {
        @Test
        @DisplayName("duplicate email -> 'Email already in use'")
        void duplicateEmail() {
            CreateStaffRequest req = new CreateStaffRequest();
            req.setEmail("dup@test.com");
            when(userRepo.existsByEmail("dup@test.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createStaff(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Email already in use");
        }

        @Test
        @DisplayName("unknown role -> 'Role not found: ...'")
        void roleNotFound() {
            CreateStaffRequest req = new CreateStaffRequest();
            req.setEmail("staff@test.com");
            req.setRoleName("ghost");
            when(userRepo.existsByEmail("staff@test.com")).thenReturn(false);
            when(roleRepo.findByRoleName("GHOST")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createStaff(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role not found: ghost");
        }
    }

    // ---- AUDIT LOGGING: writes ARE logged ----------------------------------
    @Nested
    @DisplayName("audit logging: change/edit operations write an audit entry")
    class AuditOnWrites {
        @Test
        @DisplayName("register() -> USER_REGISTERED")
        void register() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("new@test.com");
            req.setPassword("pw");
            Role role = new Role();
            role.setRoleName("S");
            when(userRepo.existsByEmail("new@test.com")).thenReturn(false);
            when(roleRepo.findByRoleName("S")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("pw")).thenReturn("hash");
            User saved = new User();
            saved.setUserId(5L);
            when(userRepo.save(any(User.class))).thenReturn(saved);

            userService.register(req);

            verify(auditLogService).log(eq(5L), eq("USER_REGISTERED"), eq("IAM"), anyString());
        }

        @Test
        @DisplayName("login() -> USER_LOGIN with the caller IP")
        void login() {
            Permission p = new Permission();
            p.setPermissionName("VIEW_SUBSCRIBER");
            activeUser.getRole().setPermissions(List.of(p));
            LoginRequest req = new LoginRequest();
            req.setEmail("user@test.com");
            req.setPassword("pw");
            when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);
            when(jwtUtil.generateToken(eq("user@test.com"), anyList())).thenReturn("token");

            userService.login(req, "1.2.3.4");

            verify(auditLogService).log(eq(1L), eq("USER_LOGIN"), eq("IAM"), eq("1.2.3.4"));
        }

        @Test
        @DisplayName("changePassword() -> PASSWORD_CHANGED")
        void changePassword() {
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setCurrentPassword("old");
            req.setNewPassword("new");
            when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
            when(passwordEncoder.encode("new")).thenReturn("newhash");

            userService.changePassword("user@test.com", req);

            verify(auditLogService).log(eq(1L), eq("PASSWORD_CHANGED"), eq("IAM"), anyString());
        }

        @Test
        @DisplayName("updateUser() by admin -> USER_UPDATED (ADMIN)")
        void updateUserAsAdmin() {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setName("New Name");
            when(userRepo.findById(1L)).thenReturn(Optional.of(activeUser));

            userService.updateUser(1L, req, true);

            verify(auditLogService).log(eq(1L), eq("USER_UPDATED"), eq("IAM"), eq("ADMIN"));
        }

        @Test
        @DisplayName("updateUser() by self -> USER_UPDATED (SELF)")
        void updateUserAsSelf() {
            UpdateUserRequest req = new UpdateUserRequest();
            req.setName("New Name");
            when(userRepo.findById(1L)).thenReturn(Optional.of(activeUser));

            userService.updateUser(1L, req, false);

            verify(auditLogService).log(eq(1L), eq("USER_UPDATED"), eq("IAM"), eq("SELF"));
        }

        @Test
        @DisplayName("updateStatus() -> USER_STATUS_CHANGED")
        void updateStatus() {
            UpdateStatusRequest req = new UpdateStatusRequest();
            req.setStatus("S");
            when(userRepo.findById(1L)).thenReturn(Optional.of(activeUser));

            userService.updateStatus(1L, req);

            verify(auditLogService).log(eq(1L), eq("USER_STATUS_CHANGED"), eq("IAM"), eq("ADMIN"));
        }

        @Test
        @DisplayName("resetPassword() -> PASSWORD_RESET")
        void resetPassword() {
            when(userRepo.findById(1L)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.encode("Default@123")).thenReturn("def");

            userService.resetPassword(1L);

            verify(auditLogService).log(eq(1L), eq("PASSWORD_RESET"), eq("IAM"), eq("ADMIN"));
        }

        @Test
        @DisplayName("createStaff() -> STAFF_ACCOUNT_CREATED")
        void createStaff() {
            CreateStaffRequest req = new CreateStaffRequest();
            req.setEmail("staff@test.com");
            req.setRoleName("CS");
            Role role = new Role();
            role.setRoleName("CS");
            when(userRepo.existsByEmail("staff@test.com")).thenReturn(false);
            when(roleRepo.findByRoleName("CS")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode("Default@123")).thenReturn("def");
            User saved = new User();
            saved.setUserId(9L);
            when(userRepo.save(any(User.class))).thenReturn(saved);

            userService.createStaff(req);

            verify(auditLogService).log(eq(9L), eq("STAFF_ACCOUNT_CREATED"), eq("IAM"), eq("ADMIN"));
        }
    }

    // ---- AUDIT LOGGING: reads/searches are NOT logged ----------------------
    @Nested
    @DisplayName("audit logging: read/search operations write NO audit entry")
    class NoAuditOnReads {
        @Test
        @DisplayName("getUserById() does not log")
        void getUserById() {
            when(userRepo.findById(1L)).thenReturn(Optional.of(activeUser));

            userService.getUserById(1L);

            verify(auditLogService, never()).log(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("getOwnProfile() does not log")
        void getOwnProfile() {
            when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(activeUser));

            userService.getOwnProfile("user@test.com");

            verify(auditLogService, never()).log(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("getAllUsers() does not log")
        void getAllUsers() {
            when(userRepo.findAll()).thenReturn(List.of(activeUser));

            userService.getAllUsers();

            verify(auditLogService, never()).log(anyLong(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("searchUsers() does not log")
        void searchUsers() {
            when(userRepo.findAll()).thenReturn(List.of(activeUser));

            userService.searchUsers(null, null, null, null, null);

            verify(auditLogService, never()).log(anyLong(), anyString(), anyString(), anyString());
        }
    }
}
