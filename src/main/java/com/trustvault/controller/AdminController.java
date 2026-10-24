package com.trustvault.controller;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.Role;
import com.trustvault.security.CustomUserDetails;
import com.trustvault.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/users")
    public String manageUsers(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<AppUser> users = userService.getAllUsers();
        model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("users", users);
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/status")
    public String toggleUserStatus(@PathVariable("id") Long userId,
                                   @RequestParam("enabled") boolean enabled,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (userId.equals(userDetails.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Administrators cannot disable their own account.");
                return "redirect:/admin/users";
            }
            userService.updateUserStatus(userId, enabled, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/role")
    public String changeUserRole(@PathVariable("id") Long userId,
                                 @RequestParam("role") Role role,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (userId.equals(userDetails.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Administrators cannot alter their own role.");
                return "redirect:/admin/users";
            }
            userService.updateUserRole(userId, role, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "User role changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
