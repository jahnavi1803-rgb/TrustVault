package com.trustvault.security;

import com.trustvault.dto.UserRegistrationDto;
import com.trustvault.entity.AppUser;
import com.trustvault.entity.Role;
import com.trustvault.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Unauthenticated request to /dashboard must redirect to /login")
    void unauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Unauthenticated request to /admin/audit must redirect to /login")
    void unauthenticatedAdminAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Registration enforces prevention of self-assigned ADMIN privileges")
    void registrationPreventsSelfAssignedAdmin() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("sneaky_user_" + System.currentTimeMillis());
        dto.setEmail("sneaky" + System.currentTimeMillis() + "@test.internal");
        dto.setFullName("Sneaky User");
        dto.setPassword("SecretPass123!@#");
        dto.setConfirmPassword("SecretPass123!@#");
        dto.setRoleChoice("ADMIN"); // Attacker attempts to self-assign ADMIN

        AppUser registered = userService.registerUser(dto);

        // Security assertion: The user must NOT receive ADMIN role
        assertThat(registered.getRole()).isNotEqualTo(Role.ROLE_ADMIN);
        assertThat(registered.getRole()).isEqualTo(Role.ROLE_OWNER);
    }

    @Test
    @DisplayName("Login with invalid credentials redirects with error parameter")
    void invalidLoginFailsGracefully() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "non_existent_user")
                        .param("password", "WrongPassword123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }
}
