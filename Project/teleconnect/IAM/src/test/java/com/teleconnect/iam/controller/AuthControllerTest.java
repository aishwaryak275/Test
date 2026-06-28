package com.teleconnect.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.iam.dto.request.ChangePasswordRequest;
import com.teleconnect.iam.dto.request.LoginRequest;
import com.teleconnect.iam.dto.request.RegisterRequest;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.exception.InvalidCredentialsException;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.security.JwtFilter;
import com.teleconnect.iam.service.AuditLogService;
import com.teleconnect.iam.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC-slice tests for {@link AuthController}, focused on exception handling:
 * the controller's own throws (no principal, user not found) and end-to-end
 * routing of service / validation exceptions through GlobalExceptionHandler.
 *
 * Security filters are disabled so we drive the controller directly; the
 * Principal is supplied (or omitted) per test to exercise the auth-guard branch.
 */
@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private AuditLogService auditLogService;
    @MockBean private UserRepository userRepo;

    private static final String BASE = "/teleConnect/iam/api/auth";

    // ---- register: @Valid -> MethodArgumentNotValidException (400) ---------
    @Test
    @DisplayName("POST /register with bad email -> 400 validation message")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("John");
        req.setEmail("not-an-email");
        req.setPassword("secret");

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: Invalid email format"));
    }

    // ---- login: service exception routed by the handler (401) --------------
    @Test
    @DisplayName("POST /login with wrong credentials -> 401 from InvalidCredentialsException")
    void login_invalidCredentials_returns401() throws Exception {
        when(userService.login(any(LoginRequest.class), anyString()))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("wrong");

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    // ---- logout: controller-level throws -----------------------------------
    @Test
    @DisplayName("POST /logout with no principal -> 401 'Not authenticated'")
    void logout_noPrincipal_returns401() throws Exception {
        mockMvc.perform(post(BASE + "/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Not authenticated"));
    }

    @Test
    @DisplayName("POST /logout with unknown principal -> 404 'User not found'")
    void logout_unknownUser_returns404() throws Exception {
        when(userRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        Principal principal = () -> "ghost@test.com";

        mockMvc.perform(post(BASE + "/logout").principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("POST /logout with valid principal -> 200 logged out")
    void logout_success_returns200() throws Exception {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@test.com");
        when(userRepo.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        Principal principal = () -> "user@test.com";

        mockMvc.perform(post(BASE + "/logout").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    // ---- changePassword: controller-level auth guard -----------------------
    @Test
    @DisplayName("PUT /changePassword with no principal -> 401 'Not authenticated'")
    void changePassword_noPrincipal_returns401() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old");
        req.setNewPassword("new");

        mockMvc.perform(put(BASE + "/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Not authenticated"));
    }

    @Test
    @DisplayName("PUT /changePassword with blank fields -> 400 validation")
    void changePassword_blankFields_returns400() throws Exception {
        Principal principal = () -> "user@test.com";

        mockMvc.perform(put(BASE + "/changePassword")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /changePassword valid -> delegates to service, 200")
    void changePassword_success_returns200() throws Exception {
        when(userService.changePassword(anyString(), any(ChangePasswordRequest.class)))
                .thenReturn(new MessageDTO("Password changed successfully"));

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old");
        req.setNewPassword("new");
        Principal principal = () -> "user@test.com";

        mockMvc.perform(put(BASE + "/changePassword")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }
}
