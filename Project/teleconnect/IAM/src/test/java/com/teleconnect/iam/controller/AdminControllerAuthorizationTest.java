package com.teleconnect.iam.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.iam.dto.request.CreateStaffRequest;
import com.teleconnect.iam.dto.request.UpdateStatusRequest;
import com.teleconnect.iam.dto.response.MessageDTO;
import com.teleconnect.iam.dto.response.RegisterResponseDTO;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.repository.RoleRepository;
import com.teleconnect.iam.security.JwtUtil;
import com.teleconnect.iam.security.SecurityConfig;
import com.teleconnect.iam.service.AuditLogService;
import com.teleconnect.iam.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @PreAuthorize coverage for {@link AdminController}, using the real
 * {@link SecurityConfig} so behaviour matches runtime. Each secured endpoint is
 * checked with its granting authority (allowed) and without it (403 from the
 * security filter chain).
 */
@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-which-is-long-enough-for-hmac-sha256-signing",
        "jwt.expiration=3600000",
        "jwt.renewal.threshold.ms=60000"
})
class AdminControllerAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private RoleRepository roleRepo;
    @MockBean private AuditLogService auditLogService;
    @MockBean private JwtUtil jwtUtil; // required by JwtFilter wired in SecurityConfig

    private static final String BASE = "/teleConnect/iam/api";

    private String staffJson() throws Exception {
        CreateStaffRequest req = new CreateStaffRequest();
        req.setName("Jane Staff");
        req.setEmail("jane@test.com");
        req.setPhone("9999999999");
        req.setRoleName("CS");
        return objectMapper.writeValueAsString(req);
    }

    private String statusJson() throws Exception {
        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus("A");
        return objectMapper.writeValueAsString(req);
    }

    // ---- POST /admin/users/createStaff : CREATE_USER -----------------------
    @Test
    @WithMockUser(authorities = "CREATE_USER")
    @DisplayName("createStaff with CREATE_USER -> allowed (201)")
    void createStaff_withPermission_created() throws Exception {
        when(userService.createStaff(any(CreateStaffRequest.class)))
                .thenReturn(new RegisterResponseDTO("Staff account created successfully"));

        mockMvc.perform(post(BASE + "/admin/users/createStaff")
                        .contentType(MediaType.APPLICATION_JSON).content(staffJson()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("createStaff without permission -> 403")
    void createStaff_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(post(BASE + "/admin/users/createStaff")
                        .contentType(MediaType.APPLICATION_JSON).content(staffJson()))
                .andExpect(status().isForbidden());
    }

    // ---- PUT /users/{id}/status : DELETE_USER ------------------------------
    @Test
    @WithMockUser(authorities = "DELETE_USER")
    @DisplayName("updateStatus with DELETE_USER -> allowed (200)")
    void updateStatus_withPermission_ok() throws Exception {
        when(userService.updateStatus(anyLong(), any(UpdateStatusRequest.class)))
                .thenReturn(new MessageDTO("User status updated to A"));

        mockMvc.perform(put(BASE + "/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content(statusJson()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("updateStatus without permission -> 403")
    void updateStatus_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(put(BASE + "/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content(statusJson()))
                .andExpect(status().isForbidden());
    }

    // ---- PUT /admin/users/{id}/resetPassword : CREATE_USER -----------------
    @Test
    @WithMockUser(authorities = "CREATE_USER")
    @DisplayName("resetPassword with CREATE_USER -> allowed (200)")
    void resetPassword_withPermission_ok() throws Exception {
        when(userService.resetPassword(1L))
                .thenReturn(new MessageDTO("Password reset to default successfully"));

        mockMvc.perform(put(BASE + "/admin/users/1/resetPassword"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("resetPassword without permission -> 403")
    void resetPassword_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(put(BASE + "/admin/users/1/resetPassword"))
                .andExpect(status().isForbidden());
    }

    // ---- GET /roles : VIEW_ALL_USERS ---------------------------------------
    @Test
    @WithMockUser(authorities = "VIEW_ALL_USERS")
    @DisplayName("getRoles with VIEW_ALL_USERS -> allowed (200)")
    void getRoles_withPermission_ok() throws Exception {
        when(roleRepo.findAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("getRoles without permission -> 403")
    void getRoles_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/roles"))
                .andExpect(status().isForbidden());
    }

    // ---- GET /roles/{roleId}/permissions : VIEW_ALL_USERS ------------------
    @Test
    @WithMockUser(authorities = "VIEW_ALL_USERS")
    @DisplayName("getRolePermissions with VIEW_ALL_USERS -> allowed (200)")
    void getRolePermissions_withPermission_ok() throws Exception {
        when(roleRepo.findById(1)).thenReturn(Optional.of(new Role()));

        mockMvc.perform(get(BASE + "/roles/1/permissions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("getRolePermissions without permission -> 403")
    void getRolePermissions_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/roles/1/permissions"))
                .andExpect(status().isForbidden());
    }

    // ---- GET /auditLogs : VIEW_AUDIT_LOGS ----------------------------------
    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOGS")
    @DisplayName("getAllLogs with VIEW_AUDIT_LOGS -> allowed (200)")
    void getAllLogs_withPermission_ok() throws Exception {
        when(auditLogService.getAllLogs(any())).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/auditLogs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("getAllLogs without permission -> 403")
    void getAllLogs_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/auditLogs"))
                .andExpect(status().isForbidden());
    }

    // ---- GET /auditLogs/user/{userId} : VIEW_AUDIT_LOGS --------------------
    @Test
    @WithMockUser(authorities = "VIEW_AUDIT_LOGS")
    @DisplayName("getLogsByUser with VIEW_AUDIT_LOGS -> allowed (200)")
    void getLogsByUser_withPermission_ok() throws Exception {
        when(auditLogService.getLogsByUser(anyLong(), any())).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/auditLogs/user/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "NONE")
    @DisplayName("getLogsByUser without permission -> 403")
    void getLogsByUser_withoutPermission_forbidden() throws Exception {
        mockMvc.perform(get(BASE + "/auditLogs/user/1"))
                .andExpect(status().isForbidden());
    }
}
