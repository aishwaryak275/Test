package com.teleconnect.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.exception.DuplicateResourceException;
import com.teleconnect.iam.repository.RoleRepository;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MVC-slice tests for {@link AdminController} exception handling. Security
 * filters and @PreAuthorize are disabled here so the focus stays on how
 * controller / service exceptions map to HTTP responses.
 */
@WebMvcTest(controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private RoleRepository roleRepo;
    @MockBean private AuditLogService auditLogService;

    private static final String BASE = "/teleConnect/iam/api";

    @Test
    @DisplayName("GET /roles/{id}/permissions unknown role -> 404 'Role not found'")
    void getRolePermissions_unknownRole_returns404() throws Exception {
        when(roleRepo.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/roles/99/permissions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Role not found"));
    }

    @Test
    @DisplayName("POST /admin/users/createStaff duplicate email -> 409 routed from service")
    void createStaff_duplicateEmail_returns409() throws Exception {
        when(userService.createStaff(any(CreateStaffRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already in use"));

        CreateStaffRequest req = new CreateStaffRequest();
        req.setName("Jane Staff");
        req.setEmail("dup@test.com");
        req.setPhone("9999999999");
        req.setRoleName("CS");

        mockMvc.perform(post(BASE + "/admin/users/createStaff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    @DisplayName("POST /admin/users/createStaff with blank fields -> 400 validation")
    void createStaff_blankFields_returns400() throws Exception {
        mockMvc.perform(post(BASE + "/admin/users/createStaff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateStaffRequest())))
                .andExpect(status().isBadRequest());
    }
}
