package com.teleconnect.iam.controller;

import com.teleconnect.iam.dto.response.UserResponseDTO;
import com.teleconnect.iam.security.JwtUtil;
import com.teleconnect.iam.security.SecurityConfig;
import com.teleconnect.iam.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @PreAuthorize coverage for {@link UserController}, using the real
 * {@link SecurityConfig} so behaviour matches runtime: method security enforces
 * the required authority and Spring Security's filter chain returns 403 for a
 * denied authenticated user. Each secured endpoint is checked with a granting
 * authority (allowed) and without it (403). Authentication is supplied by
 * {@code @WithMockUser}; the JWT filter is a no-op here (no Authorization header).
 */
@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-which-is-long-enough-for-hmac-sha256-signing",
        "jwt.expiration=3600000",
        "jwt.renewal.threshold.ms=60000"
})
class UserControllerAuthorizationTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private JwtUtil jwtUtil; // required by JwtFilter wired in SecurityConfig

    private static final String BASE = "/teleConnect/iam/api/users";

    // ---- GET /users/{id} : VIEW_SUBSCRIBER or VIEW_ALL_USERS ---------------
    @Test
    @WithMockUser(authorities = "VIEW_SUBSCRIBER")
    @DisplayName("GET /{id} with VIEW_SUBSCRIBER -> allowed (200)")
    void getUserById_withPermission_ok() throws Exception {
        when(userService.getUserById(1L)).thenReturn(new UserResponseDTO());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("GET /{id} without permission -> 403")
    void getUserById_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isForbidden());
    }

    // ---- GET /users : VIEW_ALL_USERS ---------------------------------------
    @Test
    @WithMockUser(authorities = "VIEW_ALL_USERS")
    @DisplayName("GET /users with VIEW_ALL_USERS -> allowed (200)")
    void getAllUsers_withPermission_ok() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("GET /users without permission -> 403")
    void getAllUsers_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isForbidden());
    }

    // ---- GET /users/search : VIEW_ALL_USERS or VIEW_SUBSCRIBER -------------
    @Test
    @WithMockUser(authorities = "VIEW_ALL_USERS")
    @DisplayName("GET /search with VIEW_ALL_USERS -> allowed (200)")
    void search_withPermission_ok() throws Exception {
        when(userService.searchUsers(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE + "/search").param("name", "Ali"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("GET /search without permission -> 403")
    void search_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/search").param("name", "Ali"))
                .andExpect(status().isForbidden());
    }
}
