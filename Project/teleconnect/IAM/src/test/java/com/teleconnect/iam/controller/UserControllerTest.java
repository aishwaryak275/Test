package com.teleconnect.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.iam.dto.request.UpdateUserRequest;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.UserResponseDTO;
import com.teleconnect.iam.exception.ResourceNotFoundException;
import com.teleconnect.iam.security.JwtFilter;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC-slice tests for {@link UserController}. Security filters and @PreAuthorize
 * are disabled so the focus is the controller's own logic: success responses,
 * exception routing through GlobalExceptionHandler, and the admin/non-admin
 * branch in updateUser. The Principal / Authentication is supplied per test.
 */
@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;

    private static final String BASE = "/teleConnect/iam/api/users";

    private UserResponseDTO sampleUser() {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setUserId(1L);
        dto.setName("Alice");
        dto.setEmail("alice@test.com");
        dto.setPhone("9999999999");
        dto.setRoleName("S");
        dto.setStatus("ACTIVE");
        return dto;
    }

    // ---- GET /users/me -----------------------------------------------------
    @Test
    @DisplayName("GET /me returns the caller's profile")
    void getMe_returnsProfile() throws Exception {
        when(userService.getOwnProfile("alice@test.com")).thenReturn(sampleUser());
        Principal principal = () -> "alice@test.com";

        mockMvc.perform(get(BASE + "/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    @DisplayName("GET /me unknown user -> 404 routed from service")
    void getMe_unknownUser_returns404() throws Exception {
        when(userService.getOwnProfile("ghost@test.com"))
                .thenThrow(new ResourceNotFoundException("User not found"));
        Principal principal = () -> "ghost@test.com";

        mockMvc.perform(get(BASE + "/me").principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ---- GET /users/{id} ---------------------------------------------------
    @Test
    @DisplayName("GET /{id} returns the user")
    void getUserById_returnsUser() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUser());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.roleName").value("S"));
    }

    @Test
    @DisplayName("GET /{id} unknown -> 404 routed from service")
    void getUserById_unknown_returns404() throws Exception {
        when(userService.getUserById(99L))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ---- PUT /users/{id} ---------------------------------------------------
    @Test
    @DisplayName("PUT /{id} with VIEW_ALL_USERS authority -> service called as admin")
    void updateUser_asAdmin_passesIsAdminTrue() throws Exception {
        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class), eq(true)))
                .thenReturn(new MessageDTO("User updated successfully"));

        Authentication admin = new UsernamePasswordAuthenticationToken(
                "admin@test.com", null, List.of(new SimpleGrantedAuthority("VIEW_ALL_USERS")));
        UpdateUserRequest req = new UpdateUserRequest();
        req.setName("New Name");

        mockMvc.perform(put(BASE + "/1")
                        .principal(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated successfully"));

        verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class), eq(true));
    }

    @Test
    @DisplayName("PUT /{id} without admin authority -> service called as non-admin")
    void updateUser_asNonAdmin_passesIsAdminFalse() throws Exception {
        when(userService.updateUser(eq(2L), any(UpdateUserRequest.class), eq(false)))
                .thenReturn(new MessageDTO("User updated successfully"));

        Authentication subscriber = new UsernamePasswordAuthenticationToken(
                "sub@test.com", null, List.of(new SimpleGrantedAuthority("VIEW_SUBSCRIBER")));
        UpdateUserRequest req = new UpdateUserRequest();
        req.setPhone("8888888888");

        mockMvc.perform(put(BASE + "/2")
                        .principal(subscriber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(2L), any(UpdateUserRequest.class), eq(false));
    }

    @Test
    @DisplayName("PUT /{id} unknown user -> 404 routed from service")
    void updateUser_unknown_returns404() throws Exception {
        when(userService.updateUser(eq(99L), any(UpdateUserRequest.class), eq(true)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        Authentication admin = new UsernamePasswordAuthenticationToken(
                "admin@test.com", null, List.of(new SimpleGrantedAuthority("VIEW_ALL_USERS")));

        mockMvc.perform(put(BASE + "/99")
                        .principal(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ---- GET /users --------------------------------------------------------
    @Test
    @DisplayName("GET /users returns all users")
    void getAllUsers_returnsList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleUser()));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("alice@test.com"));
    }

    // ---- GET /users/search -------------------------------------------------
    @Test
    @DisplayName("GET /search forwards filters to the service")
    void search_returnsFilteredList() throws Exception {
        when(userService.searchUsers("Ali", null, null, null, "S"))
                .thenReturn(List.of(sampleUser()));

        mockMvc.perform(get(BASE + "/search")
                        .param("name", "Ali")
                        .param("role", "S"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));

        verify(userService).searchUsers("Ali", null, null, null, "S");
    }
}
