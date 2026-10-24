package com.trustvault.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "viewer_user", roles = {"VIEWER"})
    @DisplayName("VIEWER role cannot access /admin/audit and is redirected to access-denied")
    void viewerBlockedFromAdminAudit() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "owner_user", roles = {"OWNER"})
    @DisplayName("OWNER role cannot access /admin/users and is redirected to access-denied")
    void ownerBlockedFromAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-denied"));
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN role can successfully access /admin/audit")
    void adminCanAccessAudit() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN role can successfully access /admin/users")
    void adminCanAccessUserManagement() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }
}
